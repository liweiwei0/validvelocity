package fun.lww.validvelocity;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.ToolContext;
import org.apache.velocity.tools.ToolManager;
import org.apache.velocity.tools.config.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.util.*;

/**
 * velocity模板检查
 */
@RestController
public class ValidController {

    private static final Logger log = LoggerFactory.getLogger(ValidController.class);

    /**
     * 验证模板
     * @param data1
     * @param data2
     * @return
     */
    @PostMapping("/valid")
    public String valid(String data1, String data2) {
        log.info("template: {}", data1);
        if (StringUtils.isBlank(data1)) {
            return "模板数据是空";
        }

        Properties properties = new Properties();
        properties.setProperty(RuntimeConstants.VM_LIBRARY, "templates/velocity-macro.vm,templates/assistant-table" +
                ".vm,templates/velocity-sms.vm");
        properties.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        properties.setProperty("classpath.resource.loader.class", "org.apache.velocity.runtime.resource.loader"
                + ".ClasspathResourceLoader");
        properties.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
        properties.setProperty(RuntimeConstants.OUTPUT_ENCODING, "UTF-8");

        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.init(properties);

        ToolManager toolManager = new ToolManager(true, false);
        toolManager.setVelocityEngine(velocityEngine);
        toolManager.configure(ConfigurationUtils.getGenericTools());

        ToolContext context = toolManager.createContext();
        Map<String, Object> map = parseJSON2Map(data2);
        map.forEach(context::put);

        StringWriter writer = new StringWriter();
        try {
            velocityEngine.evaluate(context, writer, "", data1);
            return writer.toString();
        } catch(ParseErrorException pee) {
            return "velocity模板错误:  " + "line: " + pee.getLineNumber() + " " + "column: " + pee.getColumnNumber();
        }
    }

    /**
     * 将json解析为map
     * @param jsonStr
     * @return
     */
    private static Map<String, Object> parseJSON2Map(String jsonStr){
        log.info("data: {}", jsonStr);
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isBlank(jsonStr)) {
            return map;
        }
        jsonStr = jsonStr.replaceAll("\r", "").replaceAll("\n", "")
                .replaceAll("\t", "");
        JSONObject json = JSONObject.fromObject(jsonStr);
        for(Object k : json.keySet()){
            Object v = json.get(k);
            if(v instanceof JSONArray){
                Iterator it = ((JSONArray)v).iterator();
                List<Map<String, Object>> list = new ArrayList<>();
                List<Object> list1 = new ArrayList<>();
                while(it.hasNext()){
                    Object json2 = it.next();
                    if (isJson(json2.toString())) {
                        list.add(parseJSON2Map(json2.toString()));
                    } else {
                        list1.add(json2.toString());
                    }
                }
                if (!list.isEmpty()) {
                    map.put(k.toString(), list);
                } else if (!list1.isEmpty()) {
                    map.put(k.toString(), list1);
                }
            } else {
                map.put(k.toString(), v);
            }
        }
        return map;
    }

    /**
     * 判断是一个json格式字符串
     * @param str
     * @return
     */
    private static boolean isJson(String str){
        try {
            JSONObject.fromObject(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
