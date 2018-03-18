package server;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;

public class HttpHandler {
	public static void httpHandler(BufferedReader clientInput, DataOutputStream serverResponse) throws IOException, ParseException {
		int type_of_method = 0;
		
		String path = new String();
		
			try {
			String first_line = clientInput.readLine();
			
			System.out.println("Clients request call:" + first_line);
			if (first_line.startsWith("HEAD"))
				type_of_method = 1;
			else if (first_line.startsWith("GET"))
				type_of_method = 2;
			else if (first_line.startsWith("PUT"))
				type_of_method = 3;
			else if (first_line.startsWith("POST"))
				type_of_method = 4;
			
			if (type_of_method == 0) {
				try {
					serverResponse.writeBytes(httpHeaderConstructor(501,"").toString());
					serverResponse.close();
					return;
				}catch (Exception e) {
					System.out.println("Error: " + e.getMessage());
			    }
			}
			
			int start = 0;
			int end = 0;
			
			for (int a = 0; a < first_line.length(); a++) {
				if (first_line.charAt(a) == ' ' && start != 0) {
					end = a;
					break;
				}
				if (first_line.charAt(a) == ' ' && start == 0) {
					start = a;
				}
			}
			
			path = first_line.substring(start +2, end);
			System.out.println("Requested file path: " + path);
			
		}catch(Exception e) {
			try {
				serverResponse.writeBytes(httpHeaderConstructor(500,"").toString());
				serverResponse.close();
				return;
			}catch( Exception err) {
				System.out.println("Error" + err.getMessage());
			}
			System.out.println("Error" + e.getMessage());
		}
		
		System.out.println("Client requested: " + new File(path).getAbsolutePath());
		checkRequestedFile(path, serverResponse);
		
		String next_line;
		Boolean host_header = false;
		while((next_line = clientInput.readLine()) != null) {
			if (next_line.startsWith("Host")) {
				host_header = true;
			}
			else if (next_line.startsWith("If-Modified-Since")) {
				if (type_of_method != 2) {
					try {
						serverResponse.writeBytes(httpHeaderConstructor(400,"").toString());
						serverResponse.close();
						return;
					}catch( Exception e) {
						System.out.println("Error" + e.getMessage());
				    }
				}
				else {
				Date inputDate = new Date();
				for (int a = 0; a < next_line.length(); a++){
					if (next_line.charAt(a)==':'){
						String temp = next_line.substring(a+2);
						inputDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").parse(temp);
						break;
					}
				}
				File file = new File(path);
				long lastModified = file.lastModified();
				Date dateLastModified = new Date(lastModified);
				SimpleDateFormat dateTemplate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
				String temp = dateTemplate.format(dateLastModified);
				Date lastModifiedDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").parse(temp);
				
				if (inputDate.compareTo(lastModifiedDate) > 0) {
					try {
						serverResponse.writeBytes(httpHeaderConstructor(304,"").toString());
						serverResponse.close();
						return;
					}catch( Exception e) {
						System.out.println("Error" + e.getMessage());
				    }
			   }
			   }
		}
	}
		if (!host_header) {
			try {
				serverResponse.writeBytes(httpHeaderConstructor(400,"").toString());
				serverResponse.close();
				return;
			}catch( Exception err) {
				System.out.println("Error" + err.getMessage());
			}
		}
		
		
	}
	private static void checkRequestedFile(String filePath, DataOutputStream srvResponse) {;
		String fileExtension = FilenameUtils.getExtension(filePath);
		
		try {
			new FileInputStream(filePath);
			System.out.println("File found and opened succesfully");
		} catch (Exception e) {
			try {
				System.out.println("Could not find file!");
				srvResponse.writeBytes(httpHeaderConstructor(404,fileExtension).toString());
				srvResponse.close();
			} catch (Exception err) {
				System.out.println("Error: Can't send failed response!");
				System.out.println("Error: " + err.getMessage());
			}
		}
		
		return;
		
	}
	
	private static StringBuilder httpHeaderConstructor(int returnCode, String typeOfFile) {
		StringBuilder sb = new StringBuilder ("HTTP/1.1 ");
		
		switch (returnCode) {
		case 200:
			sb.append("200 OK");
			break;
		case 304:
			sb.append("304 Not Modified");
			break;
		case 400:
			sb.append("400 Bad Request");
			break;
		case 403:
			sb.append("403 Forbidden");
			break;
		case 404:
			sb.append("404 Not found");
			break;
		case 500:
			sb.append("500 Server error");
			break;
		case 501:
			sb.append("501 Not implemented");
			break;
		}
		sb.append("\r\n");
		
		
		if (typeOfFile.equals("") || typeOfFile.equals("html")) {
			sb.append("Content-Type: text/html\r\n");
		}
		else {
			sb.append("Content-Type: image/"+typeOfFile+"\r\n");
		}
		
		Date date= new Date();
		SimpleDateFormat dateTemplate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		sb.append("Date: " + dateTemplate.format(date));
		
		
		
		return sb;	
	}
	
	private static StringBuilder CommandHandler(StringBuilder header, int typeOfMethod, String filePath) throws IOException {
		switch(typeOfMethod) {
		case 1:
			return header;
		case 2:
			header.append("\r\n");
			String fileExtension = FilenameUtils.getExtension(filePath);
			if(fileExtension.equals("html")) {
				BufferedReader html_file = new BufferedReader(new FileReader(filePath));
				String str;
				while ((str = html_file.readLine()) != null) {
	            header.append(str);
	            }
				html_file.close();
				return header;				
			}
			else {
				BufferedImage bufferedImage = ImageIO.read(new File(filePath));		
				byteImage = Byte	ArrayConversion.toByteArray(bufferedImage);	
	 
				System.out.println(byteImage.toString());
				return header;
				// TODO
			}
		case 3:
			File newFile = new File(putpost.txt);
			boolean createdFile = newFile.createNewFile();
			if (!createdFile) {
				newFile.delete();
				newFile.createNewFile();
			}
			
	
	      
			
			
		}
		
	}
}
