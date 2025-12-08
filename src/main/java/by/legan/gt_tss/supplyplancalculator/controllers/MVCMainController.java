package by.legan.gt_tss.supplyplancalculator.controllers;


import by.legan.gt_tss.supplyplancalculator.Data.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.ServletContext;

/**
 * MVC контроллер для обработки запросов веб-страниц.
 * Предоставляет конечные точки для главной страницы и 3D просмотрщика.
 */
@Controller
@RequestMapping("/")
public class MVCMainController {
    @Autowired
    private ServletContext servletContext;

    /**
     * Возвращает главную индексную страницу.
     *
     * @return ModelAndView для главной страницы
     */
    @GetMapping
    public ModelAndView index(){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("context", servletContext.getContextPath());
        modelAndView.setViewName("main");
        return modelAndView;
    }

    /**
     * Отображает страницу 3D просмотрщика с результатами визуализации.
     * Парсит JSON данные и передает их в шаблон просмотрщика.
     *
     * @param visualResult_DTO_json JSON строка содержащая результаты визуализации
     * @return ModelAndView для страницы 3D просмотрщика
     */
    @PostMapping(value = "/viewer",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = {MediaType.APPLICATION_ATOM_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ModelAndView view3D(@RequestParam("visualResult_DTO") String visualResult_DTO_json){
        ModelAndView modelAndView = new ModelAndView();
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .excludeFieldsWithModifiers(0x00000080) // STATIC|TRANSIENT in the default configuration
                .create();
        VisualResult_DTO visualResult_dto = gson.fromJson(visualResult_DTO_json, VisualResult_DTO.class);
        modelAndView.addObject("visualResult_DTO", visualResult_dto);
        modelAndView.addObject("context", servletContext.getContextPath());
        modelAndView.setViewName("viewer");
        return modelAndView;
    }
}
