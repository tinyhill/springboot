package com.reptile.contorller.socialSecurity;

import io.swagger.annotations.ApiOperation;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.reptile.service.accumulationfund.NingBoAccumulationfundService;
import com.reptile.service.socialSecurity.NingboSocialSecurityService;

@Controller
@RequestMapping("NingboSocialSecurityController")
public class NingboSocialSecurityController {
	@Autowired
    private NingboSocialSecurityService service;

    @RequestMapping(value = "loadImageCode",method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "加载图片验证码",notes = "参数：无")
    public Map<String,Object> loadImageCode(HttpServletRequest request){

        return service.loadImageCode(request);
    }

    @RequestMapping(value = "getDeatilMes",method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "贵阳社保",notes = "参数：身份证，密码，图片验证码")
    public Map<String,Object> getDeatilMes(HttpServletRequest request, @RequestParam("idCard") String idCard, @RequestParam("passWord")String passWord,
                                           @RequestParam("imageCode")String imageCode){

        return service.getDeatilMes(request, idCard.trim(), passWord.trim(),imageCode.trim());
    }
}