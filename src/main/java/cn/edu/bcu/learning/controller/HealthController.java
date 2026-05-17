package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("OK");
    }
}