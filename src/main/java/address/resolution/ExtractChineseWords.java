package address.resolution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class ExtractChineseWords {
    
    static Logger log = Logger.getLogger(ExtractChineseWords.class);
    
    /**
     * 翻译地址 并 抽取中文词, 输出文件为 words.data
     * 
     * @param addressFile
     */
    public static void extract(String addressFile) {
        File parentDir = new File(addressFile).getParentFile();
        File[] dataFiles = listTranslatedDataFiles(parentDir);
        
        // 如果没有已经翻译的数据，则翻译地址
        if(dataFiles == null || dataFiles.length == 0) {
            GoogleTranslator.translateAddrsInFile(addressFile);
            dataFiles = listTranslatedDataFiles(parentDir);
        }
        
        ThreadPool.COUNT = 0;
        final Pattern p = Pattern.compile("[\\u4e00-\\u9fa5]");
        final Map<String, Integer> words = Collections.synchronizedMap(new HashMap<String, Integer>());
        for(final File dataFile : dataFiles) {
            ThreadPool.getInstance().excute( new Task() {
                public void excute() {
                    try { 
                        FileInputStream fileInputStream = new FileInputStream(dataFile);
                        BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream, "UTF-8"));
                        String data = null;
                        while((data = br.readLine()) != null) {
                            data = data.replaceAll("\"", "");
                            data = data.replaceAll("\\[", "").replaceAll("\\]", "");
                            
                            String[] ss = data.split(",");
                            for(String word : ss) {
                                // 空的、全英文的、一个字的、太长的不要
                                if( word == null || word.getBytes().length == word.length()) {
                                    continue;
                                }
                                
                                // 过滤掉一些信息，比如几号几栋几单元几室几层等
                                word = word.trim().replaceAll(" ", "");
                                word = word.replaceAll("[0-9]*室", ""); 
                                word = word.replaceAll("[0-9]*单元", ""); 
                                word = word.replaceAll("[0-9]*幢", ""); 
                                word = word.replaceAll("[0-9]*号", ""); 
                                word = word.replaceAll("路[0-9]*号", ""); 
                                word = word.replaceAll("[0-9]*弄", ""); 
                                word = word.replaceAll("[0-9]*楼", "");
                                word = word.replaceAll("[0-9]*号楼", "");
                                word = word.replaceAll("[0-9]*层", "");
                                if( word.length() <= 1 || word.length() >= 10) {
                                    continue;
                                }
                                
                                int count = 0;
                                Matcher m = p.matcher(word);
                                while (m.find()) {
                                    for (int i = 0; i <= m.groupCount(); i++) {
                                        count = count + 1;
                                    }
                                }

                                if(count > 0) {
                                    Integer rate = words.get(word);
                                    if(rate == null) {
                                        rate = 0;
                                    }
                                    words.put(word, rate + 1);
                                }
                            }
                        }
                        
                        br.close();
                        log.info(dataFile.getName() + " end.");
                        ThreadPool.addCount();
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    } 
                }});
        }
        
        // 等待多线程完成
        while(ThreadPool.COUNT < dataFiles.length) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
            
        log.info("对中文按出现频率进行排序。");
        Map<String, Integer> sordedWords = sortMapByValue(words); 
        
        log.info("输出结果到文件。");        
        for (Map.Entry<String, Integer> entry : sordedWords.entrySet()) {  
            GoogleTranslator.output2File(entry.getKey() + ", " + entry.getValue(), parentDir.getPath() + "/words.data");
        }  
    }

    private static File[] listTranslatedDataFiles(File parentDir) {
        File[] dataFiles = parentDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.indexOf("result.data.") >= 0;
            }
            
        });
        return dataFiles;
    }
    
    public static Map<String, Integer> sortMapByValue(Map<String, Integer> oriMap) {  
        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();  
        
        List<Map.Entry<String, Integer>> entryList = new ArrayList<Map.Entry<String, Integer>>(oriMap.entrySet());  
        Collections.sort(entryList, new Comparator<Map.Entry<String, Integer>>() {  
                    public int compare(Entry<String, Integer> entry1, Entry<String, Integer> entry2) {  
                        return entry2.getValue() - entry1.getValue();  
                    }  
                });  
        
        for (Map.Entry<String, Integer> entry : entryList) {  
            sortedMap.put(entry.getKey(), entry.getValue());  
        }  
            
        return sortedMap;  
    }  
    
    public static void main(String[] args) {
        System.out.println("江干区\t凯旋新村".replaceAll("\t", " "));
    }
}

