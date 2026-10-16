package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.api.dto.*;
import com.genewarrior.genewarrior.config.GeneWarriorToolProperties;
import org.forester.io.parsers.nhx.NHXParser;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.data.PhylogenyDataUtil;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.BooleanSupplier;

@Service
public class GuideTreeService {
    public static final String METHOD = "tcoffee-nj-guide-v1";
    private final TcoffeeService tcoffee;
    private final ExternalToolRunner runner;
    private final GeneWarriorToolProperties properties;

    public GuideTreeService(TcoffeeService tcoffee, ExternalToolRunner runner, GeneWarriorToolProperties properties) {
        this.tcoffee = tcoffee;
        this.runner = runner;
        this.properties = properties;
    }

    public List<SequenceDto> validate(TreeRequest request) {
        require(request != null && request.requestId() != null && request.kind() != null, "Tree request is required");
        require(validId(request.sourceAlignmentId()) && validName(request.name()), "Invalid source alignment");
        var rows = request.sequences();
        require(rows != null && rows.size() >= 3 && rows.size() <= 20, "T-Coffee guide trees require 3 to 20 alignment rows");
        List<SequenceDto> sequences = new ArrayList<>();
        int width = -1;
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            require(row != null && row.value() != null, "Alignment row is required");
            if (row.value().length() > 10000)
                throw new PayloadTooLargeException("Guide trees accept at most 10,000 alignment columns");
            require(width == -1 || width == row.value().length(), "Alignment rows must have equal lengths");
            width = row.value().length();
            sequences.add(new SequenceDto(identifier(i), row.name(), request.kind(), row.value().replace("-", ""), List.of()));
        }
        return tcoffee.validate(sequences);
    }

    public GuideTreeDto create(TreeRequest request, BooleanSupplier cancelled) {
        tcoffee.requireAvailable();
        var sequences = validate(request);
        StringBuilder fasta = new StringBuilder();
        for (int i = 0; i < sequences.size(); i++)
            fasta.append('>').append(identifier(i)).append('\n').append(sequences.get(i).sequence()).append('\n');
        // Recompute the alignment guide tree from ungapped rows with the same offline MSA method.
        var output = runner.run(List.of(properties.tcoffee().path(), "-seq", "input.fasta", "-type",
                request.kind() == SequenceKind.DNA ? "dna" : "protein", "-method", TcoffeeService.METHOD,
                "-tree_mode", "nj", "-newtree", "guide.nwk", "-output", "fasta_aln", "-outfile", "aligned.fasta",
                "-n_core", "1", "-quiet"), "", Map.of("input.fasta", fasta.toString()), "guide.nwk", cancelled);
        if (output.exitCode() != 0)
            throw new ToolExecutionException("TOOL_FAILED", "T-Coffee could not build the guide tree");
        return parse(request, sequences, output.output(), tcoffee.capabilities().version());
    }

    static GuideTreeDto parse(TreeRequest request, List<SequenceDto> sequences, String newick, String version) {
        try {
            // Tool output uses generated IDs only. Bound depth/size before invoking the established parser.
            require(newick != null && newick.length() <= 20000 && newick.matches("[\\s(),:;a-zA-Z0-9_.+\\-]+"), "Invalid Newick data");
            require(newick.trim().endsWith(";") && newick.chars().filter(c -> c == ';').count() == 1, "Expected one Newick tree");
            int depth = 0;
            for (char c : newick.toCharArray()) {
                if (c == '(') require(++depth <= 40, "Tree too deep");
                if (c == ')') require(--depth >= 0, "Unbalanced tree");
            }
            require(depth == 0, "Unbalanced tree");
            var parser = new NHXParser();
            parser.setTaxonomyExtraction(NHXParser.TAXONOMY_EXTRACTION.NO);
            parser.setReplaceUnderscores(false);
            parser.setGuessRootedness(false);
            parser.setAllowErrorsInDistanceToParent(false);
            parser.setSource(newick);
            var trees = parser.parse();
            require(trees.length == 1, "Expected one tree");
            List<GuideTreeDto.Node> nodes = new ArrayList<>();
            convert(trees[0].getRoot(), null, nodes, sequences);
            List<AlignmentProvenanceDto.Source> sources = new ArrayList<>();
            for (var sequence : sequences)
                sources.add(new AlignmentProvenanceDto.Source(sequence.id(), sequence.name(),
                        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(sequence.sequence().getBytes(StandardCharsets.US_ASCII)))));
            var result = new GuideTreeDto("tree-" + request.requestId(), request.name().substring(0, Math.min(230, request.name().length())) + " [Guide tree]",
                    request.sourceAlignmentId(), request.kind(), version, METHOD, List.copyOf(nodes), List.copyOf(sources));
            validateResult(result);
            return result;
        } catch (Exception e) {
            throw new ToolExecutionException("INVALID_OUTPUT", "T-Coffee returned an invalid guide tree");
        }
    }

    private static void convert(PhylogenyNode node, Integer parent, List<GuideTreeDto.Node> nodes, List<SequenceDto> sequences) {
        require(nodes.size() < 40, "Too many tree nodes");
        Integer index = null;
        if (node.isExternal()) {
            for (int i = 0; i < sequences.size(); i++) if (identifier(i).equals(node.getName())) index = i;
            require(index != null, "Unknown leaf");
        }
        int id = nodes.size();
        double distance = node.getDistanceToParent();
        require(parent == null || distance != PhylogenyDataUtil.BRANCH_LENGTH_DEFAULT, "Missing branch length");
        nodes.add(new GuideTreeDto.Node(id, parent, index == null ? "" : sequences.get(index).name(), index,
                parent == null ? null : distance));
        for (int i = 0; i < node.getNumberOfDescendants(); i++) convert(node.getChildNode(i), id, nodes, sequences);
    }

    public static void validateResult(GuideTreeDto tree) {
        require(tree != null && validId(tree.id()) && validId(tree.sourceAlignmentId()) && validName(tree.name()) && tree.kind() != null, "Invalid guide tree");
        require(METHOD.equals(tree.method()) && tree.version() != null && tree.version().matches("[0-9][A-Za-z0-9.]{0,99}"), "Invalid tree method/version");
        require(tree.sources() != null && tree.sources().size() >= 2 && tree.sources().size() <= 20, "Invalid tree sources");
        for (var source : tree.sources())
            require(source != null && validName(source.name()) && source.sha256() != null
                    && source.sha256().matches("[a-f0-9]{64}") && (source.id() == null || validId(source.id())), "Invalid tree source");
        var nodes = tree.nodes();
        require(nodes != null && nodes.size() >= 3 && nodes.size() <= 40, "Invalid tree nodes");
        int[] children = new int[nodes.size()];
        Set<Integer> leaves = new HashSet<>();
        for (int i = 0; i < nodes.size(); i++) {
            var node = nodes.get(i);
            require(node != null && node.id() == i, "Invalid node order");
            if (i == 0) require(node.parentId() == null && node.length() == null, "Invalid tree root");
            else {
                require(node.parentId() != null && node.parentId() >= 0 && node.parentId() < i, "Invalid tree parent");
                children[node.parentId()]++;
                require(node.length() != null && Double.isFinite(node.length()) && Math.abs(node.length()) <= 1000000, "Invalid branch length");
            }
            if (node.sourceIndex() != null) {
                int index = node.sourceIndex();
                require(index >= 0 && index < tree.sources().size() && leaves.add(index)
                        && tree.sources().get(index).name().equals(node.name()), "Invalid tree leaf");
            } else require("".equals(node.name()), "Unexpected internal label");
        }
        for (int i = 0; i < nodes.size(); i++)
            require(nodes.get(i).sourceIndex() == null ? children[i] >= 2 : children[i] == 0, "Invalid tree topology");
        require(leaves.size() == tree.sources().size(), "Missing tree leaves");
    }

    private static String identifier(int i) {
        return String.format(Locale.ROOT, "seq%04d", i + 1);
    }

    private static boolean validId(String value) {
        return value != null && value.matches("[a-zA-Z0-9_-]{1,160}");
    }

    private static boolean validName(String value) {
        return value != null && !value.isBlank() && value.length() <= 255;
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalArgumentException(message);
    }
}
