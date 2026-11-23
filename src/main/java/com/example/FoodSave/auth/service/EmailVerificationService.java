package com.example.FoodSave.auth.service;
import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.dto.AuthResponse;
import com.example.FoodSave.auth.entity.EmailVerificationToken;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.repo.EmailVerificationTokenRepository;
import com.example.FoodSave.auth.repo.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    @Value("${spring.mail.username}")
    private String fromEmail;

    private final JavaMailSender mailSender;
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    private static final Base64.Encoder BASE64_ENCODER =
            Base64.getUrlEncoder().withoutPadding();
    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    public String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return BASE64_ENCODER.encodeToString(bytes);
    }

    public String hashToken(String rawToken) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes){
            String hex = Integer.toHexString(0xff & b);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String createVerificationToken(User user){
        EmailVerificationToken emailVerificationToken = new EmailVerificationToken();
        String token = generateToken();
        try {
            emailVerificationToken.setTokenHash(hashToken(token));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
        emailVerificationToken.setExpiresAt(LocalDateTime.now(ZoneId.of("Asia/Almaty")).plusMinutes(30));
        emailVerificationToken.setUser(user);
        emailVerificationToken.setUsedAt(null);
        tokenRepository.save(emailVerificationToken);
        return token;
    }

    // Sent synchronously (the app has no @EnableAsync): a delivery failure must reach the caller
    // so registration is rolled back and the user sees a clear error.
    public void sendVerificationEmail(String email, String rawToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Ваш код верификации!");
            helper.setFrom(fromEmail);

            String htmlContent = buildEmailContent(rawToken);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new MailPreparationException("Could not build verification email", e);
        }
    }


    @Transactional
    public AuthResponse verifyEmail(String rawToken) {
        String tokenHash;

        try {
            tokenHash = hashToken(rawToken);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
        EmailVerificationToken token = tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(
                        BAD_REQUEST,
                        "Недействительная ссылка подтверждения"
                ));

        if (token.getUsedAt() != null) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Ссылка уже использована"
            );
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Almaty"));

        if (!token.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Срок действия ссылки истёк"
            );
        }

        User user = token.getUser();
        user.setEmailVerified(true);

        token.setUsedAt(now);

        tokenRepository.save(token);

        String jwt = jwtService.generateToken(new UserPrincipal(user));
        return new AuthResponse(jwt);
    }

    @Transactional
    public void resendEmailCode(String email) {

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || user.isEmailVerified()) {
            return;
        }

        List<EmailVerificationToken> oldTokens =
                tokenRepository.findByUserAndUsedAtIsNull(user);

        tokenRepository.deleteAll(oldTokens);

        String rawToken = createVerificationToken(user);

        sendVerificationEmail(email, rawToken);
    }





    private String buildEmailContent(String rawToken) {

        String verificationLink =
                "http://localhost:5173/verify-email?token=" + rawToken;

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport"
                  content="width=device-width, initial-scale=1.0">
            <title>FoodSave Email Verification</title>
        </head>

        <body style="
            margin:0;
            padding:40px 16px;
            background-color:#f1f6f0;
            font-family:Arial,Helvetica,sans-serif;
            color:#183b2b;">

            <table role="presentation"
                   cellpadding="0"
                   cellspacing="0"
                   border="0"
                   style="
                       width:100%%;
                       max-width:560px;
                       margin:0 auto;
                       background:#ffffff;
                       border-radius:16px;">

                <tr>
                    <td style="padding:36px 36px 12px;">

                        <div style="
                            color:#168554;
                            font-size:28px;
                            font-weight:800;
                            letter-spacing:-1px;">
                            &#127807; foodsave
                        </div>

                        <p style="
                            font-size:12px;
                            color:#779384;
                            letter-spacing:2px;
                            margin-top:12px;">
                            SAVE FOOD. SAVE THE PLANET.
                        </p>

                    </td>
                </tr>

                <tr>
                    <td style="padding:12px 36px 20px;">

                        <h1 style="
                            font-size:28px;
                            line-height:1.3;
                            color:#153f2d;
                            margin:12px 0 20px;">
                            Welcome to FoodSave!
                        </h1>

                        <p style="
                            font-size:15px;
                            line-height:1.8;
                            color:#53675b;">
                            You're one step away from discovering
                            great food at lower prices and helping
                            reduce food waste.
                        </p>

                        <p style="
                            font-size:15px;
                            line-height:1.8;
                            color:#53675b;">
                            Please confirm your email address
                            to activate your account.
                        </p>

                    </td>
                </tr>

                <tr>
                    <td align="center"
                        style="padding:12px 36px 30px;">

                        <a href="%s"
                           style="
                               display:inline-block;
                               background-color:#168554;
                               color:#ffffff;
                               text-decoration:none;
                               font-size:15px;
                               font-weight:bold;
                               padding:17px 38px;
                               border-radius:10px;">
                            Verify Email &rarr;
                        </a>

                        <p style="
                            font-size:12px;
                            color:#8a9b90;
                            margin-top:20px;">
                            This verification link expires
                            in 30 minutes.
                        </p>

                    </td>
                </tr>

                <tr>
                    <td style="padding:0 36px 28px;">

                        <p style="
                            font-size:12px;
                            line-height:1.7;
                            color:#87998d;
                            overflow-wrap:anywhere;">
                            If the button doesn't work,
                            copy and paste this link
                            into your browser:
                        </p>

                        <a href="%s"
                           style="
                               color:#168554;
                               font-size:12px;
                               overflow-wrap:anywhere;">
                            %s
                        </a>

                    </td>
                </tr>

                <tr>
                    <td style="
                        background-color:#f8faf8;
                        padding:24px 36px;
                        border-radius:0 0 16px 16px;
                        text-align:center;">

                        <p style="
                            font-size:12px;
                            color:#89998d;
                            line-height:1.7;">
                            If you didn't create an account,
                            you can safely ignore this email.
                        </p>

                        <p style="
                            font-size:12px;
                            color:#168554;
                            font-weight:bold;">
                            FoodSave &copy; 2026
                        </p>

                        <p style="
                            font-size:11px;
                            color:#a3b0a6;">
                            Save food. Save money.
                            Save the planet.
                        </p>

                    </td>
                </tr>

            </table>
        </body>
        </html>
        """.formatted(
                verificationLink,
                verificationLink,
                verificationLink
        );
    }

}
