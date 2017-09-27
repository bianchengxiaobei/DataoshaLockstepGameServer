package com.chen.battle.message.req;

import org.apache.mina.core.buffer.IoBuffer;

import com.chen.battle.bean.Command;
import com.chen.message.Message;

public class ReqCommandMessage extends Message
{
	public Command command;
	@Override
	public int getId() {
		// TODO Auto-generated method stub
		return 2001;
	}

	@Override
	public String getQueue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServer() {
		
		return null;
	}

	@Override
	public boolean read(IoBuffer buffer) 
	{
		this.command = (Command)readBean(buffer, Command.class);
		return true;
	}

	@Override
	public boolean write(IoBuffer buffer) 
	{
		
		return true;
	}
	
}
