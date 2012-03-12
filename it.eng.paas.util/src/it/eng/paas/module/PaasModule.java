package it.eng.paas.module;

public enum PaasModule {
	AEE("aee"),
	CC("cc"),
	RR("rr"),
	PO("po");
	
	private String text;

	  PaasModule(String text) {
	    this.text = text;
	  }

	  public String getText() {
	    return this.text;
	  }
	  
	  public static PaasModule fromString(String text) {
		  if (text != null) {
			  for (PaasModule m : PaasModule.values()) {
				  if (text.equalsIgnoreCase(m.text)) {
					  return m;
				  }
		      }
		  }
		  return null;
	  }
}
