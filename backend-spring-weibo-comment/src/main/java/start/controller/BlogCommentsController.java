package start.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import common.ThreadLocalContext.ThreadLocalParam;
import common.constant.JwtConstant;
import common.result.Result;
import common.ThreadLocalContext.ThreadLocalContextHolder;
import model.entity.Blog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.BlogCommentsService;
import service.BlogService;

import java.util.Map;

@RestController
@RequestMapping("/comments")
public class BlogCommentsController {
    @Autowired
    private BlogCommentsService blogCommentsService;
    @Autowired
    private BlogService blogService;

}
