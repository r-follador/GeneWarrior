import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.io.File;
import java.io.PrintWriter;
import java.util.Locale;

// Run with: java -Djava.awt.headless=true tools/GenerateLogoGlyphs.java FONT.ttf OUTPUT.ts
// Generated outlines are normalized to a 100 x 100 box, independent of browser fonts.
class GenerateLogoGlyphs {
    public static void main(String[] args) throws Exception {
        var font = Font.createFont(Font.TRUETYPE_FONT, new File(args[0])).deriveFont(100f);
        var context = new FontRenderContext(null, true, true);
        try (var out = new PrintWriter(args[1], java.nio.charset.StandardCharsets.UTF_8)) {
            out.println("// Generated from DejaVu Sans Bold by tools/GenerateLogoGlyphs.java.");
            out.println("// License: frontend/public/licenses/dejavu.txt (also embedded in SVG exports).");
            out.println("export const glyphs: Record<string, string> = {");
            for (char letter : "ACDEFGHIKLMNPQRSTVWY".toCharArray()) {
                var shape = font.createGlyphVector(context, new char[]{letter}).getOutline();
                var box = shape.getBounds2D();
                var transform = AffineTransform.getScaleInstance(100 / box.getWidth(), 100 / box.getHeight());
                transform.translate(-box.getX(), -box.getY());
                var path = shape.getPathIterator(transform);
                var data = new StringBuilder();
                double[] points = new double[6];
                while (!path.isDone()) {
                    int segment = path.currentSegment(points);
                    data.append("MLQCZ".charAt(segment));
                    int count = switch (segment) { case 0, 1 -> 2; case 2 -> 4; case 3 -> 6; default -> 0; };
                    for (int i = 0; i < count; i++) data.append(String.format(Locale.ROOT, "%.3f ", points[i]));
                    path.next();
                }
                out.printf("  %c: '%s',%n", letter, data.toString().trim());
            }
            out.println("};");
        }
    }
}
