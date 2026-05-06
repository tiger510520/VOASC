package org.VOASC;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.PropagationEngine;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.iterators.DisposableValueIterator;

public class checkSAC {
	private final Model model;
	private final Solver solver;
	private final PropagationEngine engine;

	public checkSAC(Model model, PropagationEngine engine) {
		this.model = model;
		this.solver = model.getSolver();
		this.engine = engine;
	}

	public void checkConsistency(IntVar[] va, long sacTimeLimit, long startTime, long globalTimeLimit, boolean limit)
			throws ContradictionException {
		boolean changed; 
		IntVar[] variables = va;
		long localStartTime = System.currentTimeMillis();

		do {
			changed = false;
			for (IntVar var : variables) {
				
				if (!var.isInstantiated()) { 
					DisposableValueIterator it = var.getValueIterator(true);
					while (it.hasNext()) {
						int value = it.next();
						long elapsedGlobalTime = System.currentTimeMillis() - startTime;
						long elapsedLocalTime = System.currentTimeMillis() - localStartTime;
						if (elapsedGlobalTime > globalTimeLimit || elapsedLocalTime > sacTimeLimit) {
							System.out.println("Time out. Halting...");
							break;
						}
						if (!checkSingletonConsistency(var, value)) {
							try {
								var.removeValue(value, Cause.Null);
								engine.propagate(); 
								changed = true;
							} catch (ContradictionException e) {
								engine.flush(); 
								throw e;
							}
						}
					}
					it.dispose();
				}
			}
			if(limit) {
				changed = false;
			}
		} while (changed);
	}

	/**
	 * Checks the singleton consistency of a variable with a specific value.
	 *
	 * @param var   The variable to check.
	 * @param value The value to test.
	 * @return true if the singleton is consistent, false otherwise.
	 */
	private boolean checkSingletonConsistency(IntVar var, int value) {
		solver.getEnvironment().worldPush();
		try {
			var.instantiateTo(value, Cause.Null);
			engine.propagate();
			return true;
		} catch (ContradictionException e) {
			engine.flush(); 
			return false;
		} finally {
			solver.getEnvironment().worldPop(); 
		}
	}
}
