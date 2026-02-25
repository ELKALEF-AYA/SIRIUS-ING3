package episen.sirius.rentreceipt.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Map;

@Service
public class MinioStorageService {

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioStorageService(
            @Value("${minio.url}") String minioUrl,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey,
            @Value("${minio.bucket}") String bucketName
    ) {
        this.bucketName = bucketName;
        this.minioClient = MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
    }

    public String uploadPdf(byte[] pdfBytes, String fileName) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(
                                    new ByteArrayInputStream(pdfBytes),
                                    pdfBytes.length,
                                    -1
                            )
                            .contentType("application/pdf")
                            .build()
            );
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'upload du PDF vers MinIO", e);
        }
    }

    public String generatePresignedUrl(String fileName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(fileName)
                            .expiry(5 * 60)
                            .extraQueryParams(
                                    Map.of(
                                            "response-content-disposition",
                                            "attachment; filename=\"Rent_Receipt.pdf\""
                                    )
                            )
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération de l'URL de téléchargement", e);
        }
    }
}
