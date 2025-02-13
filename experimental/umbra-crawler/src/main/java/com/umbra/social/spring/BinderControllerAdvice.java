package com.umbra.social.spring;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

@ControllerAdvice
public class BinderControllerAdvice {

    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        String[] blacklist = new String[]{"class.module.classLoader", "class.protectionDomain"};
        dataBinder.setDisallowedFields(blacklist);
    }
}
