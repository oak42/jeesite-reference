/**
 * Copyright &copy; 2012-2016 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.thinkgem.jeesite.modules.sys.security;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.stereotype.Service;

import com.thinkgem.jeesite.common.utils.StringUtils;

/**
 * 表单验证（包含验证码）过滤类
 * @author ThinkGem
 * @version 2014-5-19
   org.apache.shiro.web.filter.authc.FormAuthenticationFilter doc：
   Requires the requesting user to be authenticated for the request to continue, and
   if they are not, forces the user to login via by redirecting them to the loginUrl you configure.
   This filter constructs a UsernamePasswordToken with the values found in username, password, and rememberMe request parameters.
   It then calls Subject.login(usernamePasswordToken), effectively automatically performing a login attempt.
   Note that the login attempt will only occur when the isLoginSubmission(request,response) is true,
   which by default occurs when the request is for the loginUrl and is a POST request.

   If the login attempt fails, the resulting AuthenticationException fully qualified class name will be set as a request attribute under the failureKeyAttribute key.
   This FQCN can be used as an i18n key or lookup mechanism to explain to the user why their login attempt failed (e.g. no account, incorrect password, etc).

   If you would prefer to handle the authentication validation and login in your own code,
   consider using the PassThruAuthenticationFilter instead,
   which allows requests to the AccessControlFilter.loginUrl to pass through to your application's code directly.
 */
@Service
public class FormAuthenticationFilter extends org.apache.shiro.web.filter.authc.FormAuthenticationFilter {

	public static final String DEFAULT_CAPTCHA_PARAM = "validateCode";		//【扣】static member，衍生类中 不能改写？static final member 自己都 不能改写？
	public static final String DEFAULT_MOBILE_PARAM = "mobileLogin";
	public static final String DEFAULT_MESSAGE_PARAM = "message";

	private String captchaParam = DEFAULT_CAPTCHA_PARAM;					// private，衍生类中 不能改写
	private String mobileLoginParam = DEFAULT_MOBILE_PARAM;
	private String messageParam = DEFAULT_MESSAGE_PARAM;

/*  原 FormAuthenticationFilter.createToken：
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
		String username = getUsername(request);
		String password = getPassword(request);
		return createToken(username, password, request, response);
	}
	现override了...
*/
	protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
		String username = getUsername(request);
		String password = getPassword(request);
		if (password==null){
			password = "";		//【扣】thinkGem坚持 对get不到的attribute 返回 空串 而非 null，(后面有些method override也体现此意图) 这样做后续的影响区别体现在哪里？【猜】null的话是否导致DB中找不到也会匹配成功？
		}
		boolean rememberMe = isRememberMe(request);
		String host = StringUtils.getRemoteAddr((HttpServletRequest)request);
		String captcha = getCaptcha(request);
		boolean mobile = isMobileLogin(request);
		return new UsernamePasswordToken(username, password.toCharArray(), rememberMe, host, captcha, mobile);	//同名class，thinkGem继承原UsernamePasswordToken改写加入了 手机登录、验证码 逻辑...
	}
	
/*  原：
	protected String getUsername(ServletRequest request) {
	    return WebUtils.getCleanParam(request, getUsernameParam());		//WebUtils.getCleanParam
	                                                                      Returns: the clean param value, or null if the param does not exist or is empty.
	}
 */
	protected String getUsername(ServletRequest request) {
		String username = super.getUsername(request);
		if (StringUtils.isBlank(username)){
			username = StringUtils.toString(request.getAttribute(getUsernameParam()), StringUtils.EMPTY);
		}
		return username;
	}

/*  原：
	protected String getPassword(ServletRequest request) {
	    return WebUtils.getCleanParam(request, getPasswordParam());
	}
*/
	@Override
	protected String getPassword(ServletRequest request) {
		String password = super.getPassword(request);
		if (StringUtils.isBlank(password)){
			password = StringUtils.toString(request.getAttribute(getPasswordParam()), StringUtils.EMPTY);
		}
		return password;
	}

/*	原：
	protected boolean isRememberMe(ServletRequest request) {
		return WebUtils.isTrue(request, getRememberMeParam());
	}
	现override了...看起来好像thinkGem认为原来的WebUtils.isTrue实现有问题 所以才自己override的？但看了下isTrue的api doc，其逻辑也差不多一样啊？【扣】
*/
	@Override
	protected boolean isRememberMe(ServletRequest request) {
		String isRememberMe = WebUtils.getCleanParam(request, getRememberMeParam());
		if (StringUtils.isBlank(isRememberMe)){
			isRememberMe = StringUtils.toString(request.getAttribute(getRememberMeParam()), StringUtils.EMPTY);	//空串""
		}
		return StringUtils.toBoolean(isRememberMe);
	}

	public String getCaptchaParam() {
		return captchaParam;
	}

	protected String getCaptcha(ServletRequest request) {
		return WebUtils.getCleanParam(request, getCaptchaParam());
	}

	public String getMobileLoginParam() {
		return mobileLoginParam;
	}
	
	protected boolean isMobileLogin(ServletRequest request) {
        return WebUtils.isTrue(request, getMobileLoginParam());
    }
	
	public String getMessageParam() {
		return messageParam;
	}
	
/**
 * 登录成功之后跳转URL
 * 【扣】作者想干什么没干的？
 *      仅仅这么写跟没写有区别么？
 *      其他很多地方也这么写，留了个stub，但其实也可以想扩展的时候再写啊...
 */
	public String getSuccessUrl() {
		return super.getSuccessUrl();
	}

/*
 	原 AuthenticationFilter.issueSuccessRedirect doc：
 	   Redirects to user to the previously attempted URL after a successful login.
 	   This implementation simply calls WebUtils.redirectToSavedRequest using the successUrl as the fallbackUrl argument to that call.
 	原 code：
    protected void issueSuccessRedirect(ServletRequest request, ServletResponse response) throws Exception {
        WebUtils.redirectToSavedRequest(request, response, getSuccessUrl());
    }
    原来的逻辑是 redirect到saved request，现thinkGem改为 redirect到successUrl...
    下面不是我注释的...
*/
	@Override
	protected void issueSuccessRedirect(ServletRequest request, ServletResponse response) throws Exception {
//		Principal p = UserUtils.getPrincipal();
//		if (p != null && !p.isMobileLogin()){
			 WebUtils.issueRedirect(request, response, getSuccessUrl(), null, true);
//		}else{
//			super.issueSuccessRedirect(request, response);
//		}
	}

/*  原：
    FormAuthenticationFilter.onLoginFailure：(Overrides: onLoginFailure in class AuthenticatingFilter)
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e,
                                     ServletRequest request, ServletResponse response) {
        if (log.isDebugEnabled()) {
            log.debug( "Authentication exception", e );
        }
        setFailureAttribute(request, e);
        //login failed, let request continue back to the login page:
        return true;
    }
    改：thinkGem加入了提示message
 */
	@Override
	protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e,
									 ServletRequest request, ServletResponse response) {
		String className = e.getClass().getName(), message = "";
		if (IncorrectCredentialsException.class.getName().equals(className)
				|| UnknownAccountException.class.getName().equals(className)){
			message = "用户或密码错误, 请重试.";
		}
		else if (e.getMessage() != null && StringUtils.startsWith(e.getMessage(), "msg:")){
			message = StringUtils.replace(e.getMessage(), "msg:", "");
		}
		else{
			message = "系统出现点问题，请稍后再试！";
			e.printStackTrace(); // 输出到控制台
		}
        request.setAttribute(getFailureKeyAttribute(), className);
        request.setAttribute(getMessageParam(), message);
        return true;
/*【鸣】 ↑ 跟踪 AuthenticatingFilter 与 FormAuthenticationFilter 的 src 与 doc，【特别是FormAuthenticationFilter.onAccessDenied 的 doc】
		 大概是 true if the request should continue to be processed; false if the subclass will handle/render the response directly.
		 这里是true，所以失败的登陆能进LoginController... */
	}
	
}