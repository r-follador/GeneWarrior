package com.genewarrior.genewarrior;

import com.genewarrior.genetools.sequenceHandling.Aminoacids;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@Controller
public class MainController {
    Logger logger = LoggerFactory.getLogger(MainController.class);

    @GetMapping("/")
    public String index(Model model) {
        //response.addHeader("Cache-Control", "max-age=600, public");
        model.addAttribute("aa", new Aminoacids());
        return "index";
    }

    @GetMapping("/share={link}")
    public String shareread(@PathVariable("link") String link, Model model) {
        //response.addHeader("Cache-Control", "max-age=600, public");
        model.addAttribute("aa", new Aminoacids());
        if (link != null && link.length() > 2)
            model.addAttribute("link", "'" + link.substring(1) + "'");
        return "index";
    }

    @Transactional
    @PostMapping("/readProject")
    public ResponseEntity<String> dnatoolsServletLoadFromDB(@RequestParam String key) {
        String out = dnaToolsService.getDatafromDB(key);
        return new ResponseEntity<>(out, HttpStatus.OK);
    }


    @GetMapping("/primer")
    public String primer(Model model) {
        //response.addHeader("Cache-Control", "max-age=600, public");
        return "primer";
    }

    @GetMapping("/share")
    public String share(Model model) {
        //response.addHeader("Cache-Control", "max-age=600, public");
        return "share";
    }

    @GetMapping("/translation")
    public String translation(Model model) {
        //response.addHeader("Cache-Control", "max-age=600, public");
        return "translation";
    }

    @Autowired
    DnaToolsService dnaToolsService;

    @PostMapping(value = "/dnatoolsServlet")
    public ResponseEntity<String> dnatoolsServlet(@RequestParam MultiValueMap<String, String> allRequestParams) {
        String out = dnaToolsService.processRequest(allRequestParams);
        return new ResponseEntity<>(out, HttpStatus.OK);
    }

    @PostMapping(value = "/dnatoolsServletDownload", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody byte[] dnatoolsServletDownload(@RequestParam String text, String filename, HttpServletResponse response) {
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        return text.getBytes();
    }


}