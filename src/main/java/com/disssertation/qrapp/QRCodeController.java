package com.disssertation.qrapp;

import com.disssertation.qrapp.models.Qrcode;
import com.disssertation.qrapp.repositories.QRCodeRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.encoder.QRCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;


@RestController
public class QRCodeController {

    private static final String QR_CODE_IMAGE_PATH = "./src/main/resources/QRCode.png";

    @Value("${qrcode.image.directory}")
    private String qrCodeImageDirectory;

    private final QRCodeRepository qrCodeRepository;

    public QRCodeController(QRCodeRepository qrCodeRepository) {
        this.qrCodeRepository = qrCodeRepository;
    }


    @GetMapping(value ="/genrateAndDownloadQRCode/{codeText}/{width}/{height}")
    public void download(
            @PathVariable("codeText") String codeText,
            @PathVariable("width") Integer width,
            @PathVariable("height") Integer height)
            throws Exception {
        QRCodeGenerator.generateQRCodeImage(codeText, width, height, QR_CODE_IMAGE_PATH);
    }


    @GetMapping(value = "/genrateQRCode/{codeText}/{width}/{height}")
    public ResponseEntity<byte[]> generateQRCode(
            @PathVariable("codeText") String codeText,
            @PathVariable("width") Integer width,
            @PathVariable("height") Integer height)
            throws Exception {
        return ResponseEntity.status(HttpStatus.OK).body(QRCodeGenerator.getQRCodeImage(codeText, width, height));
    }

    @GetMapping("/generate-qr")
    public String generateQRCode(@RequestParam("text") String text,
                                 @RequestParam("image") MultipartFile imageFile,
                                 Model model) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200);

            // Generate a unique filename for the QR code image
            String filename = UUID.randomUUID().toString() + ".png";
            Path imagePath = Path.of(qrCodeImageDirectory, filename);

            // Save the QR code image to the specified directory
            Files.createDirectories(imagePath.getParent());
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", imagePath);

            // Save the image file path to the database
            Qrcode qrCode = new Qrcode();
            qrCode.setText(text);
            qrCode.setImagePath(imagePath.toString());
            qrCodeRepository.save(qrCode);

            model.addAttribute("success", "QR code generated successfully!");
            return "qrcode";
        } catch (WriterException | IOException e) {
            model.addAttribute("error", "Failed to generate QR code: " + e.getMessage());
            return "error";
        }
    }
}
