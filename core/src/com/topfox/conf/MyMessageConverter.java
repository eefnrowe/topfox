package com.topfox.conf;

import com.topfox.common.IBean;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;

public class MyMessageConverter extends AbstractHttpMessageConverter<IBean> {


    public MyMessageConverter() {
        // 新建一个我们自定义的媒体类型application/xxx-junlin
        super(new MediaType("application", "xxx-junlin", Charset.forName("UTF-8")));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        // 表明只处理UserEntity类型的参数。
        return IBean.class.isAssignableFrom(clazz);
    }

    /**
     * 重写readlntenal 方法，处理请求的数据。代码表明我们处理由“-”隔开的数据，并转成 UserEntity类型的对象。
     */
    @Override
    protected IBean readInternal(Class<? extends IBean> clazz, HttpInputMessage inputMessage) throws IOException,
            HttpMessageNotReadableException {
        String temp = StreamUtils.copyToString(inputMessage.getBody(), Charset.forName("UTF-8"));
        String[] tempArr = temp.split("-");

//        return new IBean(tempArr[0],tempArr[1]);
        return null;

    }

    /**
     * 重写writeInternal ，处理如何输出数据到response。
     */
    @Override
    protected void writeInternal(IBean userEntity,
                                 HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
//        String out = "hello: " + userEntity.getName() + "-" + userEntity.getAddress();
        String out = "hello: ";// + userEntity.getName() + "-" + userEntity.getAddress();
        outputMessage.getBody().write(out.getBytes());
    }
}