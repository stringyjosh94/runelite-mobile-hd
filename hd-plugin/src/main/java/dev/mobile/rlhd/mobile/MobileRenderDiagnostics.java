package dev.mobile.rlhd.mobile;
import java.util.Locale;
import org.lwjgl.opengles.GLES;
import static org.lwjgl.opengles.GLES30.*;
import static org.lwjgl.opengles.EXTDisjointTimerQuery.*;
/** Sparse, non-blocking GPU timestamps. CPU figures are wall times, not pure CPU execution. */
public final class MobileRenderDiagnostics {
 private final int[][] queries=new int[2][4];private final boolean[] pending=new boolean[2];
 private boolean initialized,supported;private int slot=-1,frames;private long preparation,submit,swap,lastReport;
 private double prepSum,submitSum,swapSum;private int count;private String gpu="unavailable";
 public void prepare(){preparation=System.nanoTime();}
 private void initialize(){
  initialized=true;
  var caps=GLES.getCapabilities();
  supported=caps.GL_EXT_disjoint_timer_query && caps.glQueryCounterEXT!=0 && caps.glGetQueryObjectui64vEXT!=0;
  if(supported)supported=glGetQueryi(GL_TIMESTAMP_EXT,GL_QUERY_COUNTER_BITS_EXT)>0;
  if(supported)for(int[] group:queries)glGenQueries(group);
  System.out.println("Mobile HD diagnostics: GPU timestamps="+supported+"; sparse asynchronous sampling; CPU values are wall times");
 }
 private void poll(){
  if(!supported)return;
  if(glGetInteger(GL_GPU_DISJOINT_EXT)!=0){pending[0]=pending[1]=false;gpu="disjoint (discarded)";return;}
  for(int i=0;i<2;i++)if(pending[i] && glGetQueryObjectui(queries[i][3],GL_QUERY_RESULT_AVAILABLE)!=0){
   long[] t=new long[4];for(int k=0;k<4;k++)t[k]=glGetQueryObjectui64EXT(queries[i][k],GL_QUERY_RESULT);
   pending[i]=false;
   if(t[0]<=t[1] && t[1]<=t[2] && t[2]<=t[3])gpu=String.format(Locale.ROOT,"shadow/tiled=%.2fms scene=%.2fms blit/UI=%.2fms",(t[1]-t[0])/1e6,(t[2]-t[1])/1e6,(t[3]-t[2])/1e6);
  }
 }
 public void begin(){
  if(!initialized)initialize();poll();submit=System.nanoTime();
  if(preparation!=0)prepSum+=(submit-preparation)/1e6;
  slot=-1;
  if(supported && frames++%16==0)for(int i=0;i<2;i++)if(!pending[i]){slot=i;stamp(0);break;}
 }
 public void stamp(int stage){if(slot>=0)glQueryCounterEXT(queries[slot][stage],GL_TIMESTAMP_EXT);}
 public void beforeSwap(){stamp(3);if(slot>=0){pending[slot]=true;slot=-1;}swap=System.nanoTime();submitSum+=(swap-submit)/1e6;}
 public void end(boolean efficient,int[] resolution){
  long now=System.nanoTime();swapSum+=(now-swap)/1e6;count++;preparation=0;
  if(now-lastReport>=10_000_000_000L){
   System.out.printf(Locale.ROOT,"Mobile HD timings: efficient=%s scene=%s CPU-wall preparation=%.2fms submit=%.2fms swap/capture=%.2fms; GPU last completed sample: %s%n",efficient,java.util.Arrays.toString(resolution),prepSum/count,submitSum/count,swapSum/count,gpu);
   lastReport=now;prepSum=submitSum=swapSum=0;count=0;
  }
 }
 public void destroy(){if(supported)for(int[] group:queries)glDeleteQueries(group);initialized=supported=false;pending[0]=pending[1]=false;slot=-1;}
}
