package start.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import common.ThreadLocalContext.ThreadLocalParam;
import common.constant.JwtConstant;
import common.result.Result;
import common.ThreadLocalContext.ThreadLocalContextHolder;
import jakarta.websocket.server.PathParam;
import model.dto.BlogDTO;
import model.entity.Blog;
import model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import service.BlogService;
import service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/blog")
public class BlogController {
    @Autowired
    private BlogService blogService;
    @Autowired
    private UserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @PostMapping
    public Result createBlog(@RequestBody BlogDTO blogDTO) {
        Long userId = ThreadLocalParam.getUserId();
        Blog blog = BeanUtil.toBean(blogDTO, Blog.class);
        blog.setUserId(userId);
        blogService.save(blog);
        return Result.success("createBlog::"+blog.getId());
    }
    @GetMapping("/{id}")
    public Result readBlogById(@PathVariable Long id) {
        return Result.success(blogService.getById(id));
    }
    @GetMapping("/all")
    public Result readBlog() {
        return Result.success(blogService.list());
    }
    @PostMapping("/liked/{id}")
    public Result isliked(@PathVariable Long id) {
        Long userId = ThreadLocalParam.getUserId();
        String key = "blog:liked:" + id;
        Double liked = stringRedisTemplate.opsForZSet().score(key,userId.toString());
        if (liked == null) {
            //没点赞
            boolean success = blogService.lambdaUpdate()
                    .setSql("liked= liked + 1").eq(Blog::getId,id).update();
            if(success){
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
            return Result.success("liked::"+id);
        }else {
            //点赞过
            boolean success = blogService.lambdaUpdate()
                    .setSql("liked= liked - 1").eq(Blog::getId,id).update();
            if(success){
                stringRedisTemplate.opsForZSet().remove(key,userId.toString());
            }
            return Result.success("unliked::"+id);
        }
    }
    @GetMapping("liked/of")
    public Result likedOf(@PathParam("id") long id) {
        Long userId = ThreadLocalParam.getUserId();
        String key = "blog:liked:" + id;
        Double liked = stringRedisTemplate.opsForZSet().score(key,userId.toString());
        return Result.success(liked == null ? false : true);
    }
    @GetMapping("liked/of/all")
    public Result likedOfAll(@PathParam("id") long id) {
        String key = "blog:liked:" + id;
        return Result.success(stringRedisTemplate.opsForZSet().range(key, 0, -1));
    }
    @GetMapping("/liked/hot")
    public Result likedHot(@PathParam("id") long id) {
        String key = "blog:liked:" + id;
        Set<String> set= stringRedisTemplate.opsForZSet().range(key, 0, 2);
        List<Long> ids = set.stream().map(s -> Long.parseLong(s)).toList();
        List<User> userList = userService.listByIds(ids);
        return Result.success(userList);
    }
}
