package railo.runtime.engine;

import java.io.PrintWriter;
import java.util.Map;

import railo.commons.lang.SystemOut;
import railo.runtime.PageContext;

public class ConsoleExecutionLog implements ExecutionLog {
	
	private PrintWriter pw;
	private PageContext pc;
	private long last;
	
	/**
	 * @see railo.runtime.engine.ExecutionLog#init(railo.runtime.PageContext, java.util.Map)
	 */
	public void init(PageContext pc,Map<String,String> arguments) {
		this.pc=pc;
		String type=arguments.get("stream-type");
		if(type!=null && type.trim().equalsIgnoreCase("error"))
			pw=new PrintWriter(System.err);
		else
			pw=new PrintWriter(System.out);
		
	}
	

	/**
	 * @see railo.runtime.engine.ExecutionLog#start()
	 */
	public void start() {
		last=System.nanoTime();
	}
	
	/**
	 * @see railo.runtime.engine.ExecutionLog#endline(int)
	 */
	public void endline(int line) {
		SystemOut.print(pw, pc.getId()+":"+pc.getCurrentPageSource().getDisplayPath()+":"+line+" > "+(System.nanoTime()-last)+" ns");
		last=System.nanoTime();
	}
}
