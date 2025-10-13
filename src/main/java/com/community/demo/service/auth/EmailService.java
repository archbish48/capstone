package com.community.demo.service.auth;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;


//@Service
//@RequiredArgsConstructor
//public class EmailService {
//
//    private final JavaMailSender mailSender;
//
//    public void sendAuthCode(String toEmail, String code) {
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo(toEmail);
//        message.setSubject("[인증코드] 이메일 인증 요청");
//        message.setText("인증코드: " + code);
//
//        mailSender.send(message);
//    }
//}

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    private static final String FROM_PERSONAL = "UniHelper";

    public void sendAuthCode(String toEmail, String code) {
        try {

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");   // true: 멀티파트 메시지 허용
            helper.setFrom("${MAIL_USERNAME}", FROM_PERSONAL);  // 보내는 사람 이름 설정
            helper.setTo(toEmail);
            helper.setSubject("UniHelper 이메일 인증");      //  제목 설정
            String htmlBody = createHtmlEmailBody(code);   //  이메일 본문 설정 (true: HTML 형식임을 명시)
            helper.setText(htmlBody, true);

            //  이메일 전송
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 인증 코드를 포함하는 HTML 이메일 본문을 생성합니다.
     *
     * @param number 인증 코드
     * @return HTML 형식의 이메일 본문 문자열
     */
    private String createHtmlEmailBody(String number) {
        StringBuilder body = new StringBuilder();

        //  <h3> 태그를 사용하여 "요청하신 인증 번호입니다."를 일반 텍스트보다 크게 표시
        body.append("<h3>")
                .append("요청하신 UniHelper 이메일 인증 번호입니다.")
                .append("</h3>");

        //  인증 코드를 가장 크게 강조
        body.append("<h1>")
                .append(number)
                .append("</h1>");

        body.append("<h3>")
                .append("감사합니다.")
                .append("</h3>");

        return body.toString();
    }
}
