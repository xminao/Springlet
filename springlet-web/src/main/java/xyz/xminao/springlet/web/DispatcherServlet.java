package xyz.xminao.springlet.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 负责从IoC容器中找出所有@Controller和RestController定义的Bean
 * 扫描他们的方法，找出@GetMapping和PostMapping标注的方法，获得一个处理特定URL的处理器（抽象为Dispatcher）
 */
public class DispatcherServlet extends HttpServlet {

    // 一组用于处理特定URL的处理器
    List<Dispatcher> getDispatchers = new ArrayList<>();
    List<Dispatcher> postDispatchers = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String url = req.getRequestURI();
        // 一次匹配每个Dispatcher的URL，找到特定的处理器
        for (Dispatcher dispatcher : getDispatchers) {
            // TODO: 匹配到就处理
        }
        // 没有匹配到
        resp.sendError(404, "Not Found");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    }

    static enum ParamType {
        PATH_VARIABLE, // 路径参数，从URL提取
        REQUEST_PARAM, // URL参数，从URL Query或者表单提取
        REQUEST_BODY,  // REST请求参数，从Post传递的JSON提取
        SERVLET_VARIABLE; // 直接从DispatcherServlet提取
    }

    // 根据@RequestParam @RequestBody抽象出一个Parm类型
    class Param {
        // 参数名称
        String name;
        // 参数类型
        ParamType paramType;
        // 参数Class类型
        Class<?> classType;
        // 参数默认值
        String defaultValue;
    }

    /**
     * 特定URL的处理器
     * 返回类型包括：
     * 1. void/null: 表示内部已经处理完毕
     * 2. String: 以redirect:开头就是一个重定向
     * 3. String / byte[]: 如果配合@Responsebody就是表示返回值直接写入响应
     * 4. ModelAndView: 表示是一个MVC响应，包含Model和View名称，后序模板引擎处理后写入响应
     * 5. 其他类型：如果是 @RestController，序列化未JSON后写入响应
     * 不符合上面的直接500错误
     */
    class Dispatcher {
        // 是否返回REST
        boolean isRest;
        // 是否有@ResponseBody
        boolean isResponseBody;
        // 是否返回void
        boolean isVoid;
        // URL正则匹配
        Pattern urlPattern;
        // Bean实例
        Object controller;
        // 处理方法
        Method handlerMethod;
        // 方法参数
        Param[] methodParameters;
    }
}
