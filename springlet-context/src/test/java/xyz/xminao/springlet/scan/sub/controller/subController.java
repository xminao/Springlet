package xyz.xminao.springlet.scan.sub.controller;

import xyz.xminao.springlet.annotation.Autowired;
import xyz.xminao.springlet.annotation.Component;

@Component
public class subController {
    @Autowired
    AController aController;

    BController bController;

    @Autowired
    public void setbController(BController bController) {
        this.bController = bController;
    }
}
