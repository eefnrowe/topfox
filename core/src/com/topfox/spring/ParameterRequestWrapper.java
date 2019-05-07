package com.topfox.spring;

import com.topfox.common.SysConfigRead;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class ParameterRequestWrapper extends HttpServletRequestWrapper {
    Map<String, String[]> params;
    private final byte[] body;

    //新增头信息作用
    private final Map<String, String> customHeaders;

    public ParameterRequestWrapper(HttpServletRequest request, SysConfigRead sysConfig)  throws IOException {
        super(request);
        this.customHeaders = new HashMap<String, String>();


        String data=getBodyString(request);       //获取 前台数据

//        String underscorToCamel = environment.getProperty("top.json.submit.underscore-to-camel");
//        if (Misc.isNotNull(data) && underscorToCamel !=null && underscorToCamel.equals("true")) {
//            data = CamelHelper.jsonDataToCamel(data); //下划线的keys转为驼峰命名
//        }

        body = data.getBytes(Charset.forName("UTF-8"));
        setAttribute("___formData", data);//

        //重要, inParam 不要了, 不然出现(findString) 前台传入空的情况, mapper.xml一样匹配条件了
        //params = new HashMap(inParam);
        params = new HashMap();
    }

    public void removeParameter(String key){
        params.remove(key);
    }

    public void setParameter(String key,String value){
        params.put(key, new String[]{value});
    }
    public void setParameter(String key,String[] values){
        params.put(key, values);
    }

    @Override
    public String getParameter(String name) {
        Object v = params.get(name);
        if (v == null) {
            return null;
        } else if (v instanceof String[]) {
            String[] strArr = (String[]) v;
            if (strArr.length > 0) {
                return strArr[0];
            } else {
                return null;
            }
        } else {
            return v.toString();
        }
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return params;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        Vector l = new Vector(params.keySet());
        return l.elements();
    }

    @Override
    public String[] getParameterValues(String name) {
        return params.get(name);
    }







    //以下两个方法 解决 获取 前台 传入数据的问题  获取(post put ajax  data数据)
    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {

        final ByteArrayInputStream bais = new ByteArrayInputStream(body);

        return new ServletInputStream() {

            @Override
            public int read() throws IOException {
                return bais.read();
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }
        };
    }


    public String getBodyString(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = request.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            //TODO
            //InputStream sin = new BufferedInputStream(request.getInputStream());
            //ByteArrayOutputStream sout = new ByteArrayOutputStream();
            //int b=0;
            //while((b=sin.read())!=-1){
            //    System.out.println("#######b:"+b);
            //    sout.write(b);
            //}
            //byte[] temp = sout.toByteArray();
            //String s_ok = new String(temp,"UTF-8");
            //System.out.println(s_ok);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }



    //新增头信息作用
    public void putHeader(String name, String value){
        this.customHeaders.put(name, value);
    }
    public String getHeader(String name) {
        // check the custom headers first
        String headerValue = customHeaders.get(name);

        if (headerValue != null){
            return headerValue;
        }
        // else return from into the original wrapped object
        return ((HttpServletRequest) getRequest()).getHeader(name);
    }
    public Enumeration<String> getHeaderNames() {
        // create a set of the custom header names
        Set<String> set = new HashSet<String>(customHeaders.keySet());

        // now add the headers from the wrapped request object
        @SuppressWarnings("unchecked")
        Enumeration<String> e = ((HttpServletRequest) getRequest()).getHeaderNames();
        while (e.hasMoreElements()) {
            // add the names of the request headers into the list
            String n = e.nextElement();
            set.add(n);
        }

        // create an enumeration from the set and return
        return Collections.enumeration(set);
    }

}
