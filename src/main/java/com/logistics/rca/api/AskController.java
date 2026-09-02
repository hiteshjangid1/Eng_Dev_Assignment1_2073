package com.logistics.rca.api;

import com.logistics.rca.ai.AskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
public class AskController {

    private final AskService askService;

    public AskController(AskService askService) {
        this.askService = askService;
    }

    @GetMapping("/ask")
    public AskService.AskResult askGet(@RequestParam("q") String question) {
        return askService.ask(question);
    }

    @PostMapping("/ask")
    public AskService.AskResult askPost(@RequestBody AskBody body) {
        return askService.ask(body == null ? null : body.question());
    }

    public record AskBody(String question) {
    }
}
