
package beast.gss;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import beast.core.Distribution;
import beast.util.Randomizer;

/** class for performing a one-threaded NS as part of MultiThreadedNS */
public class NSThread extends NS {
	CountDownLatch countDown;
	Map<String, Double> particlePool;
//	CountDownLatch nsCountDown;
	
	
	@Override
	protected void initParticles() {
		super.initParticles();
		for (int i = 0; i < particleCount; i++) {
			particlePool.put(particleStates[i], particleLikelihoods[i]);
		}
	}
			
	@Override
	protected void doLoop() throws IOException {
		super.doLoop();
		countDown.countDown();
	}
	
	@Override
	protected void updateParticle(int sampleNr) {
//        nsCountDown = new CountDownLatch(1);
//        countDown.countDown();
//        try {
//			nsCountDown.await();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
        int n = particlePool.size();
        synchronized (particlePool) {
            Object [] states = particlePool.keySet().toArray();			

            //String [] states = (String []) o;
    		int i = Randomizer.nextInt(n);
    		while (particlePool.get(states[i]) < minLikelihood) {
    			i = Randomizer.nextInt(n);
    		}
    		state.fromXML((String) states[i]);
		}

		// init calculation nodes & oldLogPrior
		robustlyCalcPosterior(posterior);
		
		oldLogPrior = 0;
		for (Distribution d : samplingDistribution) {
			oldLogPrior += d.getArrayValue();
		}
		
		for (int j = 0; j < subChainLength; j++) {
			composeProposal(j + subChainLength * sampleNr);
		}
	}

	@Override
	protected void updateParticleState(int iMin, String state, double likelihood) {
		String minState = particleStates[iMin];
		synchronized (particlePool) {
			particlePool.remove(minState);
			particlePool.put(state, likelihood);
		}
		super.updateParticleState(iMin, state, likelihood);
	}

	boolean isFinished() {
		return finished;
	}
}