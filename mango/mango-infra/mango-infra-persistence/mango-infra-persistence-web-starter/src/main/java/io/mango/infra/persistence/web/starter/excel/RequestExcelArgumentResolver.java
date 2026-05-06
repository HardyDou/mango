package io.mango.infra.persistence.web.starter.excel;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

import java.util.List;

/**
 * {@link RequestExcel} 参数解析器。
 */
public class RequestExcelArgumentResolver implements HandlerMethodArgumentResolver {

    private final ExcelAdapter excelAdapter;

    public RequestExcelArgumentResolver(ExcelAdapter excelAdapter) {
        this.excelAdapter = excelAdapter;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestExcel.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        if (!List.class.isAssignableFrom(parameter.getParameterType())) {
            throw new IllegalArgumentException("@RequestExcel 参数类型必须是 List");
        }
        RequestExcel annotation = parameter.getParameterAnnotation(RequestExcel.class);
        ExcelImportContext context = ExcelImportContext.of(annotation);
        MultipartFile file = multipartFile(webRequest, context.fileName());
        Class<?> rowType = ResolvableType.forMethodParameter(parameter).getGeneric(0).resolve();
        if (rowType == null) {
            throw new IllegalArgumentException("@RequestExcel List 泛型不能为空");
        }
        List<?> rows = excelAdapter.read(file, context, rowType);
        ExcelLines.fillLineNumbers(rows, context);
        return rows;
    }

    private MultipartFile multipartFile(NativeWebRequest webRequest, String fileName) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request instanceof MultipartRequest multipartRequest) {
            MultipartFile file = multipartRequest.getFile(fileName);
            if (file != null) {
                return file;
            }
        }
        throw new IllegalStateException("未找到上传文件: " + fileName);
    }
}
