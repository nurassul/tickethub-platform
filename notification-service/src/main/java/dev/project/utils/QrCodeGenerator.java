package dev.project.utils;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class QrCodeGenerator {

    public byte[] generateQrCode(String text) throws Exception {

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        var bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 300,300);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

        return pngOutputStream.toByteArray();

    }

}
