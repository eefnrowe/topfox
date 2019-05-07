package com.topfox.common;

//import java.io.PrintWriter;
//import java.io.StringWriter;

import com.topfox.misc.Misc;

public class CommonException extends RuntimeException{
	private static final long serialVersionUID = 1471447353907832702L;
	private String code;
    private String msg;
    private Response feignResponse;

    private StringBuilder sbMsg;

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
    public Response getFeignResponse() {
        return feignResponse;
    }

//    public String getError() {
//        StringWriter sw= new StringWriter();
//        printStackTrace(new PrintWriter(sw, false));
//        return sw.toString();
//    }

    public CommonException(String code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;

    }

    public CommonException(ResponseCode responseCode){
        this(responseCode.getCode(),responseCode.getMsg());
    }

    /**
     * 服务调用服务失败,  则用此构造函数
     * @param feignResponse
     */
    public CommonException(FeignResponse feignResponse){
        this(feignResponse.getCode(),feignResponse.getMsg());
        this.feignResponse = feignResponse;
    }

    public CommonException(ResponseCode responseCode, String msg){
        this(responseCode.getCode(),msg);
    }



    public static CommonException newInstance2(ResponseCode responseCode){
        CommonString commonString= new CommonString(responseCode.getCode(),null);
        commonString.getMsg().append(responseCode.getMsg());
        return new CommonException(responseCode, commonString.getMsg().toString());
    }

    public static CommonString newInstance(ResponseCode responseCode){
        return newInstance(responseCode.getCode(), null);
    }

    public static CommonString newInstance(String responseCode){
        return newInstance(responseCode, null);
    }
    private static CommonString newInstance(String responseCode, Object target){
        return new CommonString(responseCode, target);
    }

    public static class CommonString {
        private String responseCode;
        private StringBuilder sbMsg;

        public CommonString(String code, Object target){
            this.responseCode = code;
            sbMsg = new StringBuilder();

            IRestSessionHandler restSessionHandler = ApplicationContextProvider.getBean(IRestSessionHandler.class);
            AbstractRestSession restSession = null;
            if (restSessionHandler != null) {
                restSession = restSessionHandler.get();
            }


            if (restSession != null) {
                sbMsg.append("[");
                if (Misc.isNotNull(restSession.getAppName())){
                    sbMsg.append("appcationName:").append(restSession.getAppName());
                }
                String sessionId =restSession.getSessionId();
                String executeId =restSession.getExecuteId();
                String userId    =restSession.getSysUserId();
                String userCode  =restSession.getSysUserCode();
                String userName  =restSession.getSysUserName();
                String userMobile=restSession.getSysUserMobile();

                if (sessionId!=null && sessionId.length()>2) {
                    if (sbMsg.length()>1)sbMsg.append(", ");
                    sbMsg.append("sessionId:").append(sessionId);
                }
                if (executeId!=null && executeId.length()>2) {
                    if (sbMsg.length()>1)sbMsg.append(", ");
                    sbMsg.append("executeId:").append(executeId);
                }
                if (userId!=null    && userId.length()>2   ) sbMsg.append(", userId:").append(userId);
                if (userCode!=null  && userCode.length()>2 ) sbMsg.append(", userCode:").append(userCode);
                if (userName!=null  && userName.length()>2 ) sbMsg.append(", userName:").append(userName);
                if (userMobile!=null&& userMobile.length()>2)sbMsg.append(", userMobile:").append(userMobile);
            }
            if(target !=null ) {
                sbMsg.append(", class:").append(target.getClass().getName());
            }
            sbMsg.append("]\n\r");

        }


//        public CommonString(ResponseCode responseCode, AbstractRestSession restSession, Object target) {
//            this(responseCode.getCode(), restSession, target);
//        }
//        public CommonString(String responseCode, AbstractRestSession restSession, Object target){
//            this(responseCode);
//            ///
//
//        }


//        @Deprecated
//        public CommonString append(String... values){
//            if (values==null){
//                sbMsg.append("null");
//            }
//            for (String value : values) {
//                value = value==null?"null":value;
//                sbMsg.append(value);
//            }
//            return this;
//        }

        public CommonException text(Object... values){
            if (values==null){
                sbMsg.append("null");
            }
            String tempMsg;
            for (Object value : values) {
                value = value==null?"null":value;
                /////////////////////////////////////////
                //解决有两个 的问题 [appcationName:customs-dev, executeId:190319043640P600DPJ655, userId:00384]\n\r[appcationName:customs-dev, executeId:190319043640P600DPJ655, userId:00384]
                //参数值[id]不能为空！
                tempMsg = value.toString();
                int pos =tempMsg.lastIndexOf("]\n\r");
                if (pos > 0 ) {
                    tempMsg = tempMsg.substring(pos + 3);
                }
                ////////////////////////////////////////
                sbMsg.append(tempMsg);
            }
            return new CommonException(responseCode, sbMsg.toString());
        }

        public StringBuilder getMsg(){
            return sbMsg;
        }

//        @Deprecated
//        public CommonException end(){
//            return new CommonException(responseCode, sbMsg.toString());
//        }
    }
}
