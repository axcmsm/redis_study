package com.axcmsm.api;


import com.axcmsm.dto.Result;
import com.axcmsm.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


@RestController
@RequestMapping("/api/follow")
public class FollowController {

    @Resource
    private IFollowService iFollowService;

    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id")  Long id,@PathVariable("isFollow") Boolean isFollow){
        return iFollowService.follow(id,isFollow);
    }

    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id")  Long id){
        return iFollowService.isFollow(id);
    }

}
