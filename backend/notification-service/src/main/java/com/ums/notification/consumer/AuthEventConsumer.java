/*
 * package com.ums.notification.consumer;
 * 
 * import org.springframework.amqp.rabbit.annotation.RabbitListener; import
 * org.springframework.stereotype.Component;
 * 
 * import com.ums.notification.constant.QueueConstants; import
 * com.ums.notification.event.EmailVerificationEvent; import
 * com.ums.notification.event.MfaOtpEvent; import
 * com.ums.notification.event.PasswordResetEvent; import
 * com.ums.notification.service.NotificationService;
 * 
 * import lombok.RequiredArgsConstructor;
 * 
 * @Component
 * 
 * @RequiredArgsConstructor public class AuthEventConsumer {
 * 
 * private final NotificationService notificationService;
 * 
 * @RabbitListener(queues = QueueConstants.EMAIL_VERIFICATION_QUEUE) public void
 * consumeEmailVerification(EmailVerificationEvent event) {
 * 
 * notificationService.processEmailVerification(event); }
 * 
 * @RabbitListener(queues = QueueConstants.PASSWORD_RESET_QUEUE) public void
 * consumePasswordReset(PasswordResetEvent event) {
 * 
 * notificationService.processPasswordReset(event); }
 * 
 * @RabbitListener(queues = QueueConstants.MFA_OTP_QUEUE) public void
 * consumeMfaOtp(MfaOtpEvent event) {
 * 
 * notificationService.processMfaOtp(event); } }
 */