package com.chen.battle.bean;

import org.apache.mina.core.buffer.IoBuffer;

import com.chen.message.Bean;

public class Command extends Bean
{
	public long playerId;
    public int inputCode;
	@Override
	public boolean read(IoBuffer buffer) 
	{
		this.playerId = readLong(buffer);
		this.inputCode = readInt(buffer);
		return true;
	}

	@Override
	public boolean write(IoBuffer buffer) 
	{
		writeLong(buffer, playerId);
		writeInt(buffer, inputCode);
		return true;
	}
	
}
