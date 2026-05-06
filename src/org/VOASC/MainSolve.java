package org.VOASC;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import org.chocosolver.parser.SetUpException;
import org.chocosolver.parser.flatzinc.Flatzinc;
import org.chocosolver.parser.flatzinc.ast.FGoal;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainLast;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;

public class MainSolve {
	
	public static IntVar[] dvars;
	public static boolean HLCLimit = false;
	public static boolean isCSP = false;
	public static String voh = null;
	public static String out = null;
	public static String selectedvarHeuristic = null;
	public static boolean phaseSaving = true;

	public static void main(String[] args) throws IOException {

		selectedvarHeuristic = "DWR";
		HLCLimit = true;
		out = "3600s";
		isCSP = true;
		
		String inst = null;
		if(isCSP) {
			inst = "inst/nsp_2_period_14_18.fzn";  
			voh = "VOASC";
			solveAnInstance(inst, voh, 0, out);
		}else {
			inst = "inst/2025_work-task-variation_generated-seed-1-length-16-open-14-workers-12-block-15.fzn";
			phaseSaving = true;
			voh = "VOASCcop";
			solveAnInstance(inst, voh, 0, out, phaseSaving);
		}
	}

	
	public static void solveAnInstance(String instFileName, String voh, int seed, String out) {
		Model model = createModel(instFileName);
		dvars = FGoal.geneDecVarFromList();
		if (dvars == null || dvars.length == 0) {
			int vn = model.getNbVars();
			dvars = new IntVar[vn];
			for (int i = 0; i < vn; i++) {
				dvars[i] = (IntVar) model.getVar(i);
			}
		}

		Solver solver = model.getSolver();
		Arrays.sort(dvars, Comparator.comparingInt(IntVar::getId));
		AbstractStrategy<IntVar> heu = new VOASC(dvars, seed,selectedvarHeuristic);
		solver.setLubyRestart(100, new FailCounter(solver, 1), Integer.MAX_VALUE);
		solver.setSearch(heu);
		solver.limitTime(out);
		solver.setNoGoodRecordingFromRestarts(); 
		solver.solve();
		System.out.println(solver.getMeasures().toOneLineString());

	}
	public static void solveAnInstance(String instFileName, String voh, int seed, String out, boolean ps) {
		Model model = createModel(instFileName);
		dvars = FGoal.geneDecVarFromList();
		if (dvars == null || dvars.length == 0) {
			int vn = model.getNbVars();
			dvars = new IntVar[vn];
			for (int i = 0; i < vn; i++) {
				dvars[i] = (IntVar) model.getVar(i);
			}
		}
		
		
		Solver solver = model.getSolver();
		Arrays.sort(dvars, Comparator.comparingInt(IntVar::getId));
		
		IntValueSelector valueSelector = new IntDomainMin();
		if (phaseSaving) {
			if (model.getSolver().defaultSolution() == null) {
				model.getSolver().attach(new org.chocosolver.solver.Solution(model));
			}
			valueSelector = new IntDomainLast(model.getSolver().defaultSolution(), valueSelector, null);
		}
		
		AbstractStrategy<IntVar> heu = new VOASCcop(dvars, seed,selectedvarHeuristic, valueSelector);
		solver.setLubyRestart(100, new FailCounter(solver, 1), Integer.MAX_VALUE);
		solver.setSearch(heu);
		solver.limitTime(out);
		solver.setNoGoodRecordingFromRestarts(); 
		solve(solver);
		System.out.println(solver.getMeasures().toOneLineString());
		
	}
	
	
	public static void solve(Solver solver) {
		solver.showShortStatistics();
		if (solver.hasObjective()) {
			IntVar obj = (IntVar) solver.getModel().getObjective();
			boolean isMaximize = solver.getObjectiveManager().getPolicy() == ResolutionPolicy.MAXIMIZE;
			solver.findOptimalSolution(obj, isMaximize, null);
		} else {
			solver.solve();
		}
	}

	
	public static Model createModel(String fn) {
		Model model = null;
		try {
			Flatzinc fzn = new Flatzinc();
			String[] param = { "-pa", "1", fn };
			if (fzn.setUp(param)) {
				fzn.getSettings();
				fzn.createSolver();
				fzn.buildModel();
				model = fzn.getModel();
			}
		} catch (SetUpException e) {
			throw new Error(e.getMessage());
		}
		return model;

	}

}
