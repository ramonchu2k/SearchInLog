package org.ramon.utilities;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.plaf.ListUI;

public class SearchInLogs {
	//Recursos a los que acceder
	static OutputStream out;
	static final String initialParam="ini";
	static final ArrayList<String> urls=new ArrayList<String>(){
			{
				this.add("http://server:port/logs/tomcat_1.log");
				this.add("http://server:port/logs/tomcat_2.log");
				this.add("http://server:port/logs/tomcat_3.log");
				this.add("http://server:port/logs/tomcat_4.log");
			}
		};
	
	private static HashMap getParams(String[] args){
		HashMap params=new HashMap();
		ResourceBundle bundle = ResourceBundle.getBundle("params");
		params.put(bundle.getString(initialParam), args[0]); //first param is allways searching pattern
		for(int i=1;i<args.length;i++){
			String key=bundle.getString(args[i]);//Getting param key
			if(key!=null){
				i++;
				if(i<args.length){//Getting next param (value)
					if((!bundle.containsKey(key+"Check")) || args[i].matches(bundle.getString(key+"Check"))){	//checking param format
						params.put(key,args[i]);
					}else{
						System.out.println("Error, parametro "+key+" con formato incorrecto"); //TODO get i18n
					}
				}
			}else{
				System.out.println("Parameter key not found, ignoring parameter.");
			}
		}
		return params;
	}
		
	public static void main(String[] args) {
			if(args.length>0){
				HashMap params=getParams(args); //params to get the logs.
				
				String pattern = (String)params.get("pattern");
				int windowSize = Integer.parseInt((String)params.get("window"));
				String[] fecha = null;
				String fechaParam = (String)params.get("date");;
				if(fechaParam!=null){
					fecha=fechaParam.split("-");
				}
				
				List<String> urlServers=getUrlServers(fecha);
				
				URL url=null;
				//Para cada recursos guardará en result.html las apariciones según el patron
				try {
					Path path=Paths.get("result.html");
					if(!Files.exists(path)){
						path = Files.createFile(path);
					}
					out = Files.newOutputStream(path);
					out.write("<html><body>".getBytes());
					for(String urlActual: urlServers){
						try {
							out.write(("<h2>Servidor: "+urlActual+"</h2>").getBytes());
							url = new URL(urlActual);
							out.write("<div style=\"border: 4px double #F3A320; border-radius: 10px 10px 10px 10px; padding: 15px;\">".getBytes());
							searchString(pattern, url, windowSize);
							out.write("</div>".getBytes());
							out.write(("<h3>Fin del Servidor: "+urlActual+"</h3>\n\n").getBytes());
							
						} catch (MalformedURLException e1) {
							out.write("Error url mal formada".getBytes());
							e1.printStackTrace();
						}
					}
					out.write("</html></body>".getBytes());
					out.close();
				} catch (IOException e) {
					System.out.println("Error al crear el fichero de salida");
					e.printStackTrace();
				}
			}else{
				System.out.println("Parámetros incorrectos:");
				System.out.println("Formato de parámetros: <pattern> [windowSize] [date(DD-MM-YYYY)]");
			}
			
	}
	
	private static URL getURL(String uri) throws MalformedURLException{
		Path path=Paths.get(uri);
		return new URL(uri);
	}
	
	/**TODO: hacer mi propio formateador
	 * getUrlServers generate a list of urls of log files according to date, config and server wildcards 
	 * @param fecha
	 * @return
	 */
	private static List<String> getUrlServers(String[] fecha){
		ArrayList<String> resultado=new ArrayList<String>();
		ResourceBundle bundle = ResourceBundle.getBundle("servers");
		String numServers=bundle.getString("countServers");
		if(fecha!=null){
			if(numServers.matches("\\d+")){
				for(int i=1;i<=Integer.parseInt(numServers);i++){
					resultado.add(MessageFormat.format(bundle.getString("serverDated"), i, fecha[0], fecha[1], fecha[2].substring(2, 4)));
					System.out.println(resultado.get(i-1));
				}
				return urls;
			}else{
				System.out.println("Error en configuración: número de servidores invalido");
			}
			return resultado;
		}else{
			for(int i=1;i<=Integer.parseInt(numServers);i++){
				resultado.add(MessageFormat.format(bundle.getString("server"), (i+1)/2, i));
				System.out.println(resultado.get(i-1+8));
			}
			return urls;
		}
	}
	
	private static void searchString(String pattern, URL url, int windowSize) throws IOException{
		//Abrimos el scanner para leer el fichero de la url
		Scanner scanner = null;
		try{
			  scanner = new Scanner(url.openStream());
		} catch (IOException e) {
			System.out.println("Error en leer el fichero");
			e.printStackTrace();
		}
		//Usamos una cola para llevar el buffer de las ultimas N filas 
		ArrayBlockingQueue<String> array=new ArrayBlockingQueue<>(windowSize);
		if (scanner != null) {
		  String line;
		  while (scanner.hasNextLine()) {
		    line = scanner.nextLine();
		    //Si lo encontramos imprimimos las N anteriores filas y las N siguientes
		    if (line.contains(pattern)) {
		    	out.write("<div style=\"border: 4px double #F3A320; border-radius: 10px 10px 10px 10px; padding: 15px;\">".getBytes());
		    	for(String prevLine:array){
		    		out.write((prevLine+"<br/ >").getBytes());
		    	}
		    	
		    	out.write(("<b>"+line+"</b><br/ >").getBytes());
		    	
		    	int counter=windowSize;
		    	while(scanner.hasNextLine() && counter>0){
		    		line=scanner.nextLine();
		    		out.write((line+"<br/ >").getBytes());
		    		counter--;
		    	}
		    	out.write("</div>".getBytes());
			}
		    //Si se llena el buffer sacamos el elemento más antiguo
		    array.offer(line);
		    if(array.remainingCapacity()==0){
		    	array.poll();
		    }
		  }
		  scanner.close();
		}
	}

}
