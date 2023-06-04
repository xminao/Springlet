package xyz.xminao.springlet.scan.sub.controller;

import xyz.xminao.springlet.annotation.Autowired;
import xyz.xminao.springlet.annotation.Component;

@Component
public class AController {
    AController(@Autowired BController b, @Autowired CController c) {
    }
}
