package org.VOASC;

import java.util.Arrays;

import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.loop.monitors.IMonitorDownBranch;
import org.chocosolver.solver.search.loop.monitors.IMonitorRestart;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.selectors.variables.DomOverWDegRef;
import org.chocosolver.solver.search.strategy.selectors.variables.FailureBased;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelector;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
public class VOASCcop extends AbstractStrategy<IntVar> implements IMonitorDownBranch, IMonitorRestart, IMonitorSolution {

	private VariableSelector<IntVar> varSelector;
	private IntValueSelector valueSelector;
	public Solver solver;
	int decisionPathSize = 0;
	public static boolean HLCPlus = false;
	IntVar selectedVarDown = null;

	public static double[] dlcAccumulationFE;
	public static double[] scAccumulationFE;
	public static int[] dlcPropNum;
	public static int[] scPropNum;
	boolean[] selectedEachVar;
	long before;
	long after;
	int currentVarIndex = 0;
	long startTime = 0;
	int restartCount = 0;

	boolean justFoundSolution = false;
	boolean scSampled = false;
	boolean dlcSampled = false;
	int hardState = 1000;
	int dlcSampRestNum = 100;

	public VOASCcop(IntVar[] vars, long seed,String heu, IntValueSelector valueSelector) {
		super(vars);
		if (heu.equals("FRBA")) {
			this.varSelector = new FailureBased<>(vars, seed, 2);
		} else if (heu.equals("DWR")) {
			this.varSelector = new DomOverWDegRef<>(vars, seed);
		}else {
			System.out.println("!!!!!!! NO varHeuristic setting  !!!!!!");
		}
		this.valueSelector = valueSelector;
		solver = vars[0].getModel().getSolver();
		solver.plugMonitor(this);
		dlcAccumulationFE = new double[vars.length];
		scAccumulationFE = new double[vars.length];
		dlcPropNum = new int[vars.length];
		scPropNum = new int[vars.length];
		selectedEachVar = new boolean[vars.length];
		before = 0;
		after = 0;
	}

	@Override
	public boolean init() {
		return varSelector.init();
	}


	@Override
	public Decision<IntVar> getDecision() {
		// TODO Auto-generated method stub
		IntVar selectedVar = varSelector.getVariable(vars);
		if(dlcSampled) {
			int scBetter = 0;
			int dlcBetter = 0;
			boolean noneSelected = false;

			int zeroCountGAC = 0;
			int zeroCountSAC = 0;
			for (int t = 0; t < vars.length; t++) {
				if (vars[t] == selectedVar) {
					currentVarIndex = t; 
				}
			}
			
			if (scSampled) {
				for (int i = 0; i < vars.length; i++) {
					if (dlcPropNum[i] != 0) {
						zeroCountGAC++;
					} else {
						break;
					}
					if (scPropNum[i] != 0) {
						zeroCountSAC++;
					}
					if (vars[i] == selectedVar) {
						currentVarIndex = i; 
					}else {
						break;
					}
				}
			}

			if (scSampled && zeroCountGAC == vars.length && zeroCountSAC == vars.length) {
				for (int a = 0; a < vars.length; a++) {
					if (vars[a] == selectedVar) {
						double DLC = dlcAccumulationFE[a] / dlcPropNum[a];
						double SC = scAccumulationFE[a] / scPropNum[a];
						if (DLC >= SC) {
							HLCPlus = false;
							break;
						} else {
							HLCPlus = true;
							break;
						}
					}
				}
			}else if (scSampled) {
				for (int a = 0; a < vars.length; a++) {
					if (dlcPropNum[a] > 0 && scPropNum[a] > 0) {
						double DLC = dlcAccumulationFE[a] / dlcPropNum[a];
						double SC = scAccumulationFE[a] / scPropNum[a];
						if (DLC >= SC) {
							dlcBetter++;
						} else {
							scBetter++;
						}
					}
					if (vars[a] == selectedVar) {
						if (dlcPropNum[a] > 0 && scPropNum[a] > 0) {
							double AC1 = dlcAccumulationFE[a] / dlcPropNum[a];
							double SAC1 = scAccumulationFE[a] / scPropNum[a];
							if (AC1 >= SAC1) {
								HLCPlus = false;
								break;
							} else {
								HLCPlus = true;
								break;
							}
						}else if (dlcPropNum[a] > 0 && scPropNum[a] == 0) {
							HLCPlus = true;
							break;
						}else if (dlcPropNum[a] == 0 && scPropNum[a] > 0) {
							HLCPlus = false;
							break;
						} else {
							noneSelected = true;
						}
					}
				}
				if (noneSelected) {
					if (scBetter >= dlcBetter) {
						HLCPlus = true;
					} else {
						HLCPlus = false;
					}
				}
			} else {
				HLCPlus = false;
			}
			selectedEachVar[currentVarIndex] = HLCPlus;
		}
		return computeDecision(selectedVar);
	}

	@Override
	public Decision<IntVar> computeDecision(IntVar variable) {
		if (variable == null || variable.isInstantiated()) {
			return null;
		}
		int value = valueSelector.selectValue(variable);
		return variable.getModel().getSolver().getDecisionPath().makeIntDecision(variable,
				DecisionOperatorFactory.makeIntEq(), value);
	}

	@Override
	public void beforeDownBranch(boolean left) {
		// TODO Auto-generated method stub
		if (dlcSampled) {
			if (!left) {
				if (solver.getDecisionPath().size() < decisionPathSize) { 
					selectedVarDown = (IntVar) solver.getDecisionPath().getLastDecision().getDecisionVariable();
					for (int d = 0; d < vars.length; d++) {
						if (vars[d] == selectedVarDown) {
							currentVarIndex = d;
							HLCPlus = selectedEachVar[d];
							break;
						}
					}
				}
			}
			before = 0;
			after = 0;
			for (Variable var : vars) {
				before += var.getDomainSize();
			}
			decisionPathSize = solver.getDecisionPath().size();  
			startTime = System.nanoTime();
		}
	}

	@Override
	public void afterDownBranch(boolean left) {
		// TODO Auto-generated method stub
		if (dlcSampled) {
			for (Variable var : vars) {
				after += var.getDomainSize();
			}
			if (HLCPlus) {
				long duration = System.nanoTime() - startTime;
				long filteredNum = before - after;
				scPropNum[currentVarIndex]++;
				if (duration <= 1) {
					scAccumulationFE[currentVarIndex] += filteredNum;
				} else {
					scAccumulationFE[currentVarIndex] += (double) filteredNum / duration;
				}
			} else {
				long duration = System.nanoTime() - startTime;
				long filteredNum = before - after;
				dlcPropNum[currentVarIndex]++;
				if (duration <= 1) {
					dlcAccumulationFE[currentVarIndex] += filteredNum;
				} else {
					dlcAccumulationFE[currentVarIndex] += (double) (filteredNum) / duration;
				}
			}
		}
	}
	
	
	@Override
	public void afterRestart() {
		// TODO Auto-generated method stub
		restartCount++;
		int startRestart = hardState - dlcSampRestNum;
		if (!dlcSampled && restartCount >= startRestart) {
			dlcSampled = true;
		}
		if (dlcSampled && restartCount == hardState) {
			scSampled = true;
		}
	}
	
	
	@Override
	public void onSolution() {
		// TODO Auto-generated method stub
		if (dlcSampled) {
			dlcSampled = false;
			scSampled = false;
			justFoundSolution = true;
			HLCPlus = false;
			Arrays.fill(dlcAccumulationFE, 0.0);
			Arrays.fill(scAccumulationFE, 0.0);
			Arrays.fill(dlcPropNum, 0);
			Arrays.fill(scPropNum, 0);
			Arrays.fill(selectedEachVar, false);
		}
		restartCount = 0;
	}
	
}
