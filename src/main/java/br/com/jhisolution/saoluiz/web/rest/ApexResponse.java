package br.com.jhisolution.saoluiz.web.rest;

import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;

import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.common.util.StringUtils;
import me.chanjar.weixin.mp.api.WxMpInMemoryConfigStorage;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.WxMpServiceImpl;
import me.chanjar.weixin.mp.bean.WxMpCustomMessage;
import me.chanjar.weixin.mp.bean.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.WxMpXmlOutTextMessage;

@RestController
@RequestMapping("/api")
public class ApexResponse {

	private final Logger log = LoggerFactory.getLogger(AccountResource.class);

	protected WxMpInMemoryConfigStorage config;
	protected WxMpService wxMpService;
	protected WxMpMessageRouter wxMpMessageRouter;

	@PostConstruct
	public void init() {

	    config = new WxMpInMemoryConfigStorage();
	    config.setAppId("..."); // 设置微信公众号的appid
	    config.setSecret("..."); // 设置微信公众号的app corpSecret
	    config.setToken("..."); // 设置微信公众号的token
	    config.setAesKey("..."); // 设置微信公众号的EncodingAESKey

	    wxMpService = new WxMpServiceImpl();
	    wxMpService.setWxMpConfigStorage(config);

	    WxMpMessageHandler handler = new WxMpMessageHandler() {
	      public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage, Map<String, Object> context, WxMpService wxMpService) {
	        WxMpXmlOutTextMessage m
	            = WxMpXmlOutMessage.TEXT().content("测试加密消息").fromUser(wxMessage.getToUserName())
	            .toUser(wxMessage.getFromUserName()).build();
	        return m;
	      }

			@Override
			public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage, Map<String, Object> context, WxMpService wxMpService,
					WxSessionManager sessionManager) throws WxErrorException {
				// TODO Auto-generated method stub
				return null;
			}
	    };

	    wxMpMessageRouter = new WxMpMessageRouter(wxMpService);
	    wxMpMessageRouter
	        .rule()
	        .async(false)
	        .content("哈哈") // 拦截内容为“哈哈”的消息
	        .handler(handler)
	        .end();

	  
	}
	
	@GetMapping("/apexResponse/sendMessage")
    @Timed
	public void sendMessage() throws Exception {
		
		/*WxMpInMemoryConfigStorage config = new WxMpInMemoryConfigStorage();
		config.setAppId(appId); // 设置微信公众号的appid
		config.setSecret(secret); // 设置微信公众号的app corpSecret
		config.setToken(token); // 设置微信公众号的token
		config.setAesKey(aesKey); // 设置微信公众号的EncodingAESKey
*/
		WxMpService wxService = new WxMpServiceImpl();
		wxService.setWxMpConfigStorage(config);

		// 用户的openid在下面地址获得 
		// https://mp.weixin.qq.com/debug/cgi-bin/apiinfo?t=index&type=用户管理&form=获取关注者列表接口%20/user/get 
		String openid = "...";
		WxMpCustomMessage message = WxMpCustomMessage.TEXT().toUser(openid).content("Hello World").build();
		wxService.customMessageSend(message);
		
	}
	@GetMapping("/apexResponse")
    @Timed
    public void apexResponse(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.debug("REST request to check if the current user is authenticated in WeChat");
        
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        String signature = request.getParameter("signature");
        String nonce = request.getParameter("nonce");
        String timestamp = request.getParameter("timestamp");

        if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
          // 消息签名不正确，说明不是公众平台发过来的消息
          response.getWriter().println("非法请求");
          return;
        }

        String echostr = request.getParameter("echostr");
        if (StringUtils.isNotBlank(echostr)) {
          // 说明是一个仅仅用来验证的请求，回显echostr
          response.getWriter().println(echostr);
          return;
        }

        String encryptType = StringUtils.isBlank(request.getParameter("encrypt_type")) ?
            "raw" :
            request.getParameter("encrypt_type");

        if ("raw".equals(encryptType)) {
          // 明文传输的消息
          WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(request.getInputStream());
          WxMpXmlOutMessage outMessage = wxMpMessageRouter.route(inMessage);
          response.getWriter().write(outMessage.toXml());
          return;
        }

        if ("aes".equals(encryptType)) {
          // 是aes加密的消息
          String msgSignature = request.getParameter("msg_signature");
          WxMpXmlMessage inMessage = WxMpXmlMessage.fromEncryptedXml(request.getInputStream(), config, timestamp, nonce, msgSignature);
          WxMpXmlOutMessage outMessage = wxMpMessageRouter.route(inMessage);
          response.getWriter().write(outMessage.toEncryptedXml(config));
          return;
        }

        response.getWriter().println("不可识别的加密类型");
        return;
      
    }
	
}
