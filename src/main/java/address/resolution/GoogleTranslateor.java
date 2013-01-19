package address.resolution;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

public class GoogleTranslateor {
    
    static Logger log = Logger.getLogger(GoogleTranslateor.class);
    
    static String RESULT_FILE_PATH = "D:/temp/address/en/result.data";
    static String RESULT_KEY_FILE_PATH = "D:/temp/address/en/result.key";
    
    static Set<String> keySet = new HashSet<String>();
    static String REPEAT_ADDR_TAG = "exsits";
    
    static {
    	File resultKeyFile = new File(RESULT_KEY_FILE_PATH);
    	if( !resultKeyFile.exists() ) {
    		resultKeyFile.getParentFile().mkdirs();
    	} 
    	else {
    		try {
                InputStream inputStream = new FileInputStream(resultKeyFile);
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String data = null;
                while((data = br.readLine()) != null) {
                	keySet.add(data); 
                }
                br.close();
            } catch (Exception e) {
                log.error("读取rusult key文件到内存时出错。", e);
            } 
    	}
    }
 
	public static String translate(String addressCN) {
		Integer addrCode = addressCN.hashCode();
		if(keySet.contains(addrCode.toString())) {
			return REPEAT_ADDR_TAG;
		}
		
	    String urlEncodedAddress;
        try {
            urlEncodedAddress = URLEncoder.encode(addressCN, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        	throw new RuntimeException("对中文地址进行URLEncoder时异常", e);
        }
        
        String googleURI = "http://translate.google.cn/translate_a/t?client=t&text=" + urlEncodedAddress + 
                "&hl=zh-CN&sl=zh-CN&tl=en&ie=UTF-8&oe=UTF-8&multires=1&otf=1&ssel=0&tsel=0&otf=1&pc=1&ssel=0&tsel=0&sc=1";
        
        HttpClient client = new HttpClient(); 
        GetMethod method = new GetMethod(googleURI);
        method.setRequestHeader("REQUEST-TYPE", "text/javascript");
	    
        try {
            client.executeMethod(method);
            int statusCode = method.getStatusCode();

            InputStream responseBodyAsStream = method.getResponseBodyAsStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(responseBodyAsStream, "UTF-8"));
            
            StringBuffer buffer = new StringBuffer();
            String line;
            while ((line = in.readLine()) != null) {
            	buffer.append(line);
            }
            String responseContent = buffer.toString();
            log.debug("google translate response content: " + responseContent);
            
            if (statusCode == HttpStatus.SC_OK) {
				output2File( addrCode + " ||| " + responseContent, RESULT_FILE_PATH);
            	output2File( addrCode, RESULT_KEY_FILE_PATH );
            	
                int beginIndex = responseContent.indexOf("\"") + 1;
                int endIndex   = responseContent.indexOf("\"", beginIndex);
                if( endIndex > beginIndex ) {
                    String result = responseContent.substring(beginIndex, endIndex);
                    log.debug("google translate result: " + result);
                    return result;
                }
            } 
            else {
                log.error("请求连接失败");
            }
        } catch(Exception e) {
            throw new RuntimeException("调用Google翻译时异常", e);
        } finally {
            method.releaseConnection();
        }
        
        return null;
	}

	private static void output2File(Object content, String filePath) {
		BufferedWriter bw = null;
		try {
			OutputStream outputStream = new FileOutputStream(filePath, true);
			bw = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));  
			bw.write(content.toString());
			bw.newLine();
			bw.flush();  
		} catch (Exception e) {
			throw new RuntimeException("写入翻译结果到文件时出错", e);
		} finally {
			try {
				if(bw != null) {
					bw.close();
				}
			} catch (IOException e) {
			}
		}
	}
}
