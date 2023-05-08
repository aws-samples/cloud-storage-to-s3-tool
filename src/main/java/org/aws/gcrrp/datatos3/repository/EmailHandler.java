package org.aws.gcrrp.datatos3.repository;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import org.aws.gcrrp.datatos3.utils.ENVConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailHandler {

    static Logger logger = LoggerFactory.getLogger(EmailHandler.class);
    // Replace sender@example.com with your "From" address.
    // This address must be verified with Amazon SES.
//    static final String FROM = "jiatingcool@gmail.com";

    // Replace recipient@example.com with a "To" address. If your account
    // is still in the sandbox, this address must be verified.
//    static final String TO = "jiatin@amazon.com";
//    @Value("${alarm.email.from}")
//    private String emailFrom;
//
//    @Value("${alarm.email.to}")
//    private String emailTo;

//    @Value("${aws.access-key}")
//    private String awsAccessKey;
//
//    @Value("${aws.secret-key}")
//    private String awsSecretKey;

    // The configuration set to use for this email. If you do not want to use a
    // configuration set, comment the following variable and the
    // .withConfigurationSetName(CONFIGSET); argument below.
//    static final String CONFIGSET = "ConfigSet";

    // The subject line for the email.
    static final String SUBJECT = "Amazon SES test (AWS SDK for Java)";

//    // The HTML body for the email.
//    static final String HTMLBODY = "<h1>Amazon SES test (AWS SDK for Java)</h1>"
//            + "<p>This email was sent with <a href='https://aws.amazon.com/ses/'>"
//            + "Amazon SES</a> using the <a href='https://aws.amazon.com/sdk-for-java/'>"
//            + "AWS SDK for Java</a>";

    // The email body for recipients with non-HTML email clients.
    static final String TEXTBODY = "This email was sent through Amazon SES "
            + "using the AWS SDK for Java.";

    public void sendEmail(String subject, String textBody) {
        try {
//            AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(
//                    new BasicAWSCredentials(awsAccessKey, awsSecretKey));
            AmazonSimpleEmailService simpleEmailService = AmazonSimpleEmailServiceClientBuilder.standard()
//                    .withCredentials(credentialsProvider)
                    .withRegion(ENVConfig.awsRegion)
                    .build();

            SendEmailRequest request = new SendEmailRequest()
                    .withDestination(
                            new Destination().withToAddresses(ENVConfig.emailTo))
                    .withMessage(new com.amazonaws.services.simpleemail.model.Message()
                            .withBody(new Body()
                                    // .withHtml(new Content()
                                    // .withCharset("UTF-8").withData(HTMLBODY))
                                    .withText(new Content()
                                            .withCharset("UTF-8").withData(textBody)))
                            .withSubject(new Content()
                                    .withCharset("UTF-8").withData(subject)))
                    .withSource(ENVConfig.emailFrom);
            // Comment or remove the next line if you are not using a
            // configuration set
//                    .withConfigurationSetName(CONFIGSET);
            simpleEmailService.sendEmail(request);
            logger.debug("Email sent!");
        } catch (Exception ex) {
            logger.error("The email was not sent. Error message: "
                    + ex.getMessage());
        }
    }
}
