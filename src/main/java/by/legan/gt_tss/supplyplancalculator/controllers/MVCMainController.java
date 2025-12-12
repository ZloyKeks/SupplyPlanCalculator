package by.legan.gt_tss.supplyplancalculator.controllers;


import by.legan.gt_tss.supplyplancalculator.Data.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.util.StreamUtils;

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
     * Исключает /index.html, который обслуживается как статический ресурс.
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

    /**
     * Обрабатывает запросы к /index.html для отображения 3D просмотрщика.
     * Возвращает статический HTML файл из viewer/build.
     * Параметр fileName будет доступен в React приложении через window.location.search.
     *
     * @return ResponseEntity с содержимым статического index.html
     */
    @GetMapping("/index.html")
    public ResponseEntity<byte[]> indexHtml() {
        try {
            // Загружаем статический ресурс из classpath
            ClassPathResource resource = new ClassPathResource("static/index.html");
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            // Читаем содержимое файла
            InputStream inputStream = resource.getInputStream();
            byte[] bytes = StreamUtils.copyToByteArray(inputStream);
            inputStream.close();
            
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
