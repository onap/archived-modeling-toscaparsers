package org.onap.tosca.checker.test;

import java.net.URI;

import org.onap.tosca.checker.Checker;
import org.onap.tosca.checker.Target;
import org.onap.tosca.checker.Catalog;
import org.onap.tosca.checker.CSARRepository;

public class CSAR {

  public static void main(String[] theArgs) {

    if (theArgs.length == 0) {
      System.err.println("You need to specify a CSAR archive to process");
      return;
    }

		try {
			CSARRepository repo = new CSARRepository(theArgs[0], new URI(theArgs[0]));
			Checker checker = new Checker();
			checker.setTargetLocator(repo.getTargetLocator());
			checker.check(repo.mainTarget());
			
			Catalog cat = checker.catalog();
			for (Target t: cat.targets()) {
				System.err.println(t.getLocation() + "\n" + cat.importString(t) + "\n" + t.getReport());
			}
		}
		catch (Exception x) {
			x.printStackTrace();
		}
  }

}
