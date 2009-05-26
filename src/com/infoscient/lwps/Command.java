package com.infoscient.lwps;

public interface Command {
	public void execute(String[] args);
	
	public String getDescription();
}
