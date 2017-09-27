package com.chen.battle.message.res;

import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;

import com.chen.battle.bean.Command;
import com.chen.message.Message;

public class ResFramesCommandMessage extends Message
{
	public int frameCount;
	public List<Command> commands;
	@Override
	public int getId() {
		// TODO Auto-generated method stub
		return 2000;
	}

	@Override
	public String getQueue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean read(IoBuffer buffer) 
	{
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean write(IoBuffer buffer) 
	{
		writeInt(buffer, frameCount);
		writeInt(buffer, this.commands.size());
		for (int i=0;i<this.commands.size();i++)
		{
			writeBean(buffer, this.commands.get(i));
		}
		return true;
	}
	
}
