package com.looksee.pageBuilder.gcp;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.looksee.pageBuilder.models.enums.BrowserType;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * Handles uploading files to Google Cloud Storage
 */
@Retry(name = "gcp")
@Service
public class GoogleCloudStorage {
	
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(GoogleCloudStorage.class);
	private final Storage storage;
    private final String bucketName;
    private final String publicUrl;

	//private static String bucket_name     = "look-see-data";
	
	@Autowired
    public GoogleCloudStorage(Storage storage, GoogleCloudStorageProperties gcsProperties) {
        this.storage = storage;
        this.bucketName = gcsProperties.getBucketName();
        this.publicUrl = gcsProperties.getPublicUrl();
    }

	/**
	 * Uploads HTML content to Google Cloud Storage
	 * @param content
	 * @param key
	 * @return
	 * @throws IOException 
	 */
	public String uploadHtmlContent(String content, String key) throws IOException {
        BlobId blobId = BlobId.of(bucketName, key);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType("text/html")
            .setCacheControl("public, max-age=31536000")
            .build();
        
        try (WriteChannel writer = storage.writer(blobInfo)) {
            writer.write(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
        }
        return publicUrl + "/" + key;
    }

	/**
	 * Retrieves HTML content from Google Cloud Storage
	 * @param gcsUrl
	 * @return
	 */
    public String getHtmlContent(String gcsUrl) {
        String key = gcsUrl.replace(publicUrl + "/", "");
        Blob blob = storage.get(bucketName, key);
        return new String(blob.getContent(), StandardCharsets.UTF_8);
    }
	
	/**
	 * Saves an image to Google Cloud Storage
	 * @param image
	 * @param domain
	 * @param checksum
	 * @param browser
	 * @return
	 * @throws IOException
	 */
	public String saveImage(BufferedImage image,
								   String domain,
								   String checksum,
								   BrowserType browser
   ) throws IOException {
		assert image != null;
		assert domain != null;
		assert !domain.isEmpty();
		assert checksum != null;
		assert !checksum.isEmpty();
		assert browser != null;
		
		Storage storage = StorageOptions.getDefaultInstance().getService();
		Bucket bucket = storage.get(bucketName);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write( image, "png", baos );
		byte[] imageInByte = baos.toByteArray();
		baos.close();
		String stripped_domain = domain.replace(".", "").replace("/", "").replace(":", "").replace("https", "").replace("http", "");
		String key = stripped_domain+checksum+browser;
		String file_name = key+".png";
		Blob blob = bucket.get(file_name);
		if(blob != null && blob.exists()) {
        	return blob.getMediaLink();
        }
		
		//blob = bucket.create(key+".png", imageInByte);
		BlobId blobId = BlobId.of(bucketName, file_name);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/png").build();
		try (WriteChannel writer = storage.writer(blobInfo)) {
			writer.write(ByteBuffer.wrap(imageInByte, 0, imageInByte.length));
		}
		
		blob = bucket.get(file_name);
		if(blob != null && blob.exists()) {
        	return blob.getMediaLink();
        }
		else {
			throw new IOException("Couldn't find blob after upload");
		}
    }
	
	/**
	 * Retrieves an image from Google Cloud Storage
	 * @param domain
	 * @param element_key
	 * @param browser
	 * @return
	 * @throws IOException
	 */
	public BufferedImage getImage(String domain,
										 String element_key,
										 BrowserType browser
	) throws IOException {
		assert domain != null;
		assert !domain.isEmpty();
		assert element_key != null;
		assert !element_key.isEmpty();
		assert browser != null;
		
		Storage storage = StorageOptions.getDefaultInstance().getService();
		Bucket bucket = storage.get(bucketName);


		String host_key = org.apache.commons.codec.digest.DigestUtils.sha256Hex(domain);
		Blob blob = bucket.get(host_key+""+element_key+browser+".png");
		InputStream inputStream = Channels.newInputStream(blob.reader());

        return ImageIO.read(inputStream);
    }
	
	/**
	 * Retrieves an image from a URL
	 * @param image_url
	 * @return
	 * @throws IOException
	 */
	public BufferedImage getImage(String image_url) throws IOException {
		assert image_url != null;
		assert !image_url.isEmpty();
		
//		Storage storage = StorageOptions.getDefaultInstance().getService();
//		Bucket bucket = storage.get(bucketName);

//		Blob blob = bucket.get(image_url);
		return ImageIO.read(new URL(image_url));
    }
}