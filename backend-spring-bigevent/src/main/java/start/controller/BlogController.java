package start.controller;

import common.result.Result;
import model.dto.BlogDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BlogController {
    @PostMapping
    public Result createBlog(@RequestBody BlogDTO blogDTO) {
        return Result.success(blogDTO);
    }
}
