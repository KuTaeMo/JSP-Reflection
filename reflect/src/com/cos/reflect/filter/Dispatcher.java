package com.cos.reflect.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cos.reflect.anno.RequestMapping;
import com.cos.reflect.controller.UserController;

public class Dispatcher implements Filter {

	private boolean isMatching = false;
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;

		// System.out.println("컨텍스트패스 : " + request.getContextPath()); // 프로젝트 시작주소
		// System.out.println("식별자주소 : " + request.getRequestURI()); // 끝주소
		// System.out.println("전체주소 : " + request.getRequestURL()); // 전체주소

		// /user 파싱하기
		String endPoint = request.getRequestURI().replaceAll(request.getContextPath(), "");
		System.out.println("엔드포인트 : " + endPoint);

		// 1단계 분기시키기

		UserController userController = new UserController();
//		if (endPoint.equals("/join")) { // http://localhost:8000/reflect/join
//			userController.join();
//		} else if (endPoint.equals("/login")) {
//			userController.login();
//		} else if (endPoint.equals("/user")) {
//			userController.user();
//		}

		// 2단계

		// 리플렉션 -> 메서드를 런타임 시점에서 찾아내서 실행
		Method[] methods = userController.getClass().getDeclaredMethods(); // 그 파일에 메서드만!!
//		for (Method method : methods) {
//			// System.out.println(method.getName());
//			if (endPoint.equals("/" + method.getName())) {
//				try {
//					method.invoke(userController);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}

		// 3단계

		// 리플렉션 - Annotation 사용
		for (Method method : methods) { // 4바퀴 (join, login, user, hello)
			Annotation annotation = method.getDeclaredAnnotation(RequestMapping.class);
			RequestMapping requestMapping = (RequestMapping) annotation;
			//System.out.println(requestMapping.value());

			if (requestMapping.value().equals(endPoint)) {
				isMatching = true;
				try {
					Parameter[] params = method.getParameters();
					String path = null;
					if(params.length != 0) {	// 파라미터가 있는 경우
						System.out.println("params[0].getType() : "+params[0].getType());
						Object dtoInstance = params[0].getType().newInstance(); // /user/login => LoginDto, /user/join => JoinDto
						
						setData(dtoInstance, request);
						path = (String) method.invoke(userController, dtoInstance);
					}else {
						path = (String)method.invoke(userController);
					}
					RequestDispatcher dis = request.getRequestDispatcher(path); // 필터를 다시 안탐!
					dis.forward(request, response);								// 내부적으로 동작 sendRedirect는 필터 탐
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
		}
		if (isMatching == false) {

			response.setContentType("text/html; charset=utf-8");
			PrintWriter out = response.getWriter();
			out.println("잘못된 주소 요청입니다. 404 에러");
			out.flush();

		}
	}
	private <T> void setData(T instance, HttpServletRequest request) {
		Enumeration<String> keys = request.getParameterNames(); // 크기 : 2 (username, password)
		while (keys.hasMoreElements()) { // 2번 돈다.
			String key = (String) keys.nextElement();
			String methodKey = keyToMethodKey(key); // setUsername

			Method[] methods = instance.getClass().getDeclaredMethods(); // 5개(login 일 경우- toString, get 2개 set 2개

			for (Method method : methods) {
				if (method.getName().equals(methodKey)) {
					try {
						method.invoke(instance, request.getParameter(key));
					} catch (Exception e) {
						e.printStackTrace();
						try {
							int value = Integer.parseInt(request.getParameter(key));
							method.invoke(instance, value);
						} catch (Exception e2) {
							e2.printStackTrace();
						}
						System.out.println("신경쓸 필요없는 별거 아닌 int 파싱 문제");
					}
				}
			}
		}
	}

	private String keyToMethodKey(String key) {
		String firstKey = "set";
		String upperKey = key.substring(0, 1).toUpperCase();
		String remainKey = key.substring(1);

		return firstKey + upperKey + remainKey;
	}
}
