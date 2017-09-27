package com.chen.battle.structs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.chen.battle.bean.Command;
import com.chen.battle.manager.BattleManager;
import com.chen.battle.message.res.ResEnterSceneMessage;
import com.chen.battle.message.res.ResFramesCommandMessage;
import com.chen.battle.message.res.ResGamePrepareMessage;
import com.chen.battle.message.res.ResReConnectMessage;
import com.chen.battle.message.res.ResSceneLoadedMessage;
import com.chen.battle.message.res.ResSelectHeroMessage;
import com.chen.player.structs.Player;
import com.chen.server.BattleServer;
import com.chen.utils.MessageUtil;

public class BattleContext extends BattleServer
{
	private Logger log = LogManager.getLogger(BattleContext.class);
	private EBattleType battleType;
	private EBattleServerState battleState = EBattleServerState.eSSBS_SelectHero;
	private long battleId;
	public int mapId;
	private long battleStateTime;
	public long battleHeartBeatTime;
	private long lastCheckPlayTimeout;
	private long battleFinishProtectTime = 0;
	private BattleUserInfo[] m_battleUserInfo = new BattleUserInfo[maxMemberCount];
	public List<Command> commandList = new ArrayList<>();
	public int infuenceFrameCount = 0;
	public ResFramesCommandMessage framesMessage = new ResFramesCommandMessage();
	public static final int maxMemberCount = 6; 
	public static final int timeLimit = 200000;
	public static final int prepareTimeLimit = 5000;
	public static final int loadTimeLimit = 100000;
	public EBattleType getBattleType() {
		return battleType;
	}
	public void setBattleType(EBattleType battleType) {
		this.battleType = battleType;
	}
	public long getBattleId() {
		return battleId;
	}
	public void setBattleId(long battleId) {
		this.battleId = battleId;
	}
	public EBattleServerState getBattleState() {
		return battleState;
	}
	public BattleUserInfo[] getM_battleUserInfo() {
		return m_battleUserInfo;
	}
	public void setM_battleUserInfo(BattleUserInfo[] m_battleUserInfo) {
		this.m_battleUserInfo = m_battleUserInfo;
	}
	public BattleContext(EBattleType type, long battleId,int mapId)
	{
		super("战斗-"+battleId);
		this.battleId = battleId;
		this.mapId = mapId;
		this.battleType = type;
	}
	
	@Override
	protected void init() 
	{
         System.out.println("BattleContent:Init");
	}
	@Override
	public void run()
	{
		while (this.battleState != EBattleServerState.eSSBS_Finished)
		{
			this.OnHeartBeat(System.currentTimeMillis(), 100);
			if (this.battleState == EBattleServerState.eSSBS_Finished)
			{
				log.debug("战斗结束");
				BattleManager.getInstance().allBattleMap.remove(this.battleId);
				BattleManager.getInstance().mServers.remove(this.battleId, this);				
			}
			try {
				Thread.sleep(50);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public void OnHeartBeat(long now,long tickSpan)
	{
		boolean res = CheckPlayTimeout(now);
		if (res)
		{
			//战斗结束直接返回
			return;
		}
		this.checkSelectHeroTimeout();
		this.checkPrepareTimeout();
		this.checkLoadingTimeout(now,tickSpan);
		this.DoPlayHeartBeat(now,tickSpan);
	}
	public void EnterBattleState(Player player)
	{
		boolean isReconnect = player.isReconnect();
		if (isReconnect)
		{
			//通知重新连接信息
		}
		log.info("玩家"+player.getId()+"确认加入战斗房间，当前战斗状态:"+battleState.toString());
		//以后再扩展开选择符文等
	}
	/**
	 * 玩家确认选择该英雄
	 * @param player
	 * @param heroId
	 */
	public void AskSelectHero(Player player,int heroId)
	{
		BattleUserInfo info = getUserBattleInfo(player);
		info.selectedHeroId = heroId;
		info.bIsHeroChoosed = true;
		ResSelectHeroMessage msg = new ResSelectHeroMessage();
		msg.playerId = player.getId();
		msg.heroId = heroId;
		MessageUtil.tell_battlePlayer_message(this,msg);
	}
	/**
	 * 玩家发送加载完成消息
	 */
	public void EnsurePlayerLoaded(Player player)
	{
		BattleUserInfo data = this.getUserBattleInfo(player);
		data.bIsLoadedComplete = true;
		ResSceneLoadedMessage msg = new ResSceneLoadedMessage();
		msg.m_playerId = player.getId();
		//说明是重新连接进入的
		if (this.battleState == EBattleServerState.eSSBS_Playing)
		{
			//发送0帧到当前帧
			
		}
		MessageUtil.tell_battlePlayer_message(this, msg);
	}
	public void checkSelectHeroTimeout()
	{
		if (this.battleState != EBattleServerState.eSSBS_SelectHero)
		{
			return ;
		}
		boolean ifAllUserSelect = true;
		for (int i=0; i<maxMemberCount; i++)
		{
			if (this.m_battleUserInfo[i] != null)
			{
				if (this.m_battleUserInfo[i].bIsHeroChoosed == false)
				{
					ifAllUserSelect = false;
					break;
				}
			}
		}
		//等待时间结束
		if (ifAllUserSelect || (System.currentTimeMillis() - this.battleStateTime) >= timeLimit)
		{
			for (int i = 0; i < maxMemberCount; i++) {
				if (this.m_battleUserInfo[i] != null)
				{
					if (false == this.m_battleUserInfo[i].bIsHeroChoosed) {
						//如果还没有选择神兽，就随机选择一个
						if (this.m_battleUserInfo[i].selectedHeroId == -1)
						{
							this.m_battleUserInfo[i].selectedHeroId = randomPickHero(this.m_battleUserInfo[i].sPlayer.canUserHeroList);
						}
						this.m_battleUserInfo[i].bIsHeroChoosed = true;
						//然后将选择该神兽的消息广播给其他玩家
						ResSelectHeroMessage msg = new ResSelectHeroMessage();
						msg.heroId = this.m_battleUserInfo[i].selectedHeroId;
						msg.playerId = this.m_battleUserInfo[i].sPlayer.player.getId();
						MessageUtil.tell_battlePlayer_message(this, msg);
					}
				}
			}
			//选择神兽阶段结束，改变状态，进入准备状态
			setBattleState(EBattleServerState.eSSBS_Prepare,true);
		}
	}
	public void checkLoadingTimeout(long now,long timeSpan)
	{
		if (this.battleState != EBattleServerState.eSSBS_Loading)
		{
			return ;
		}
		boolean bIfAllPlayerConnect = true;
		//时间未到，则检查是否所有玩家已经连接
		if (System.currentTimeMillis() - this.battleStateTime < loadTimeLimit)
		{
			for (int i=0;i<this.m_battleUserInfo.length;i++)
			{
				if (this.m_battleUserInfo[i] != null)
				{
					if (this.m_battleUserInfo[i].bIsLoadedComplete == false)
					{
						bIfAllPlayerConnect = false;
						break;
					}
				}
			}
		}
		if (bIfAllPlayerConnect == false)
		{
			return;
		}
		//战斗开始				
		this.setBattleState(EBattleServerState.eSSBS_Playing,false);
	}
	public boolean CheckPlayTimeout(long now)
	{
		if (this.lastCheckPlayTimeout == 0)
		{
			this.lastCheckPlayTimeout = now;
			return false;
		}
		//每隔5秒检测一次
		if (now - this.lastCheckPlayTimeout < 5000)
		{
			return false;
		}
		this.lastCheckPlayTimeout = now;
		boolean bAllUserOffline = true;
		for (int i=0;i<maxMemberCount;i++)
		{
			if (this.m_battleUserInfo[i] != null)
			{
				SSPlayer player = this.m_battleUserInfo[i].sPlayer;
				//如果有一个人连上去的话，就没有所有人断线
				if (player != null && player.bIfConnect == true)
				{
					bAllUserOffline = false;
					break;
				}
			}		
		}
		//如果玩家在线的话，战斗保护时间重置
		if (bAllUserOffline == false)
		{
			this.battleFinishProtectTime = 0;
		}
		if (bAllUserOffline && this.battleFinishProtectTime == 0)
		{
			this.battleFinishProtectTime = now + 5000;
		}
		if (bAllUserOffline && now > this.battleFinishProtectTime)
		{
			log.debug("所有玩家离线，战斗结束");
			Finish();
			return true;
		}
		return false;
	}
	public void checkPrepareTimeout()
	{
		if (this.battleState != EBattleServerState.eSSBS_Prepare)
		{
			return ;
		}
		if (System.currentTimeMillis() - this.battleStateTime > prepareTimeLimit)
		{
			this.setBattleState(EBattleServerState.eSSBS_Loading, true);
		}
	}
	public void DoPlayHeartBeat(long now,long tick)
	{
		if (this.battleState != EBattleServerState.eSSBS_Playing)
		{
			return;
		}
		if (System.currentTimeMillis() - this.battleHeartBeatTime > tick)
		{
			this.framesMessage.frameCount = this.infuenceFrameCount;
			this.framesMessage.commands = this.commandList;
			this.infuenceFrameCount++;
			MessageUtil.tell_battlePlayer_message(this, framesMessage);
			this.commandList.clear();
			this.battleHeartBeatTime = System.currentTimeMillis();
		}
	}
	/**
	 * 改变游戏状态
	 * @param state
	 * @param isSendToClient
	 */
	public void setBattleState(EBattleServerState state,boolean isSendToClient)
	{
		this.battleState = state;
		this.battleStateTime = System.currentTimeMillis();
		if (isSendToClient)
		{
			switch (state) {
			case eSSBS_Prepare:
				//通知客户端开始进入准备状态
				ResGamePrepareMessage pre_msg = new ResGamePrepareMessage();
				pre_msg.setTimeLimit(prepareTimeLimit);
				MessageUtil.tell_battlePlayer_message(this, pre_msg);
				break;
			case eSSBS_Loading:
				//通知客户端开始加载场景
				ResEnterSceneMessage scene_msg = new ResEnterSceneMessage();
				MessageUtil.tell_battlePlayer_message(this, scene_msg);
				break;			
			default:
				break;
			}
		}
	}
	/**
	 * 战斗结束
	 */
	public void Finish()
	{
		if (this.battleState == EBattleServerState.eSSBS_Finished)
		{
			return;
		}
		for (int i=0; i<this.m_battleUserInfo.length; i++)
		{
			if (this.m_battleUserInfo[i] == null)
			{
				continue;
			}
			this.m_battleUserInfo[i].sPlayer.player.getBattleInfo().reset();
		}
		//通知客户端战斗结束
		setBattleState(EBattleServerState.eSSBS_Finished,true);
		//通知客户端那方赢了
		
	}
	public void AddPlayerCommand(Command command)
	{
		this.commandList.add(command);
	}
	/**
	 * 取得随机神兽
	 * @param pickHeroList
	 * @param camType
	 * @return
	 */
	private int randomPickHero(Set<Integer> pickHeroList)
	{
		List<Integer> canChooseList = new ArrayList<Integer>();
		if (pickHeroList == null || pickHeroList.size() == 0)
		{
			System.out.println("没有英雄可以选择");
		}
		for (int heroId : pickHeroList) 
		{
			canChooseList.add(heroId);
		}
		return canChooseList.get((int) (Math.random()*canChooseList.size()));		
	}
	/**
	 * 根据玩家取得玩家数据
	 * @param player
	 * @return
	 */
	public BattleUserInfo getUserBattleInfo(Player player)
	{
		if (player == null)
		{
			return null;
		}
		for (int i=0; i<this.m_battleUserInfo.length; i++)
		{
			if (this.m_battleUserInfo[i] == null)
			{
				continue;
			}
			if (this.m_battleUserInfo[i].sPlayer.player.getId() == player.getId())
			{
				return this.m_battleUserInfo[i];
			}
		}
		return null;
	}
	/**
	 * 玩家离线
	 * @param player
	 */
	public void OnUserOffline(Player player)
	{
		BattleUserInfo info = this.getUserBattleInfo(player);
		if (info != null)
		{			
			//移除消息通信
			info.sPlayer.bIfConnect = false;
			info.bIsLoadedComplete = false;
			info.bReconnect = true;
			if (battleState == EBattleServerState.eSSBS_Playing)
			{
				info.offlineTime = System.currentTimeMillis();
			}		
		}
	}
	/**
	 * 玩家重新连接战斗
	 * @param player
	 */
	public void OnEnterBattleState(Player player)
	{
		BattleUserInfo info = this.getUserBattleInfo(player);
		if (info != null)
		{
			info.sPlayer.bIfConnect = true;
			//需要重新连接
			if (info.bReconnect == true)
			{
				//发送给客户端重新连接的消息
				ResReConnectMessage message = new ResReConnectMessage();
				message.battleState = this.battleState.getValue();
				message.battleId = this.battleId;
				message.mapId = this.mapId;
				message.playerId = player.getId();
				for (int i=0;i<this.m_battleUserInfo.length;i++)
				{
					if (this.m_battleUserInfo[i] == null)
					{
						continue;
					}
					ReConnectInfo info2 = new ReConnectInfo();
					info2.playerId = this.m_battleUserInfo[i].sPlayer.player.getId();
					info2.heroId = this.m_battleUserInfo[i].selectedHeroId;
					info2.nickName = this.m_battleUserInfo[i].sPlayer.player.getName();
					message.ReConnectInfo.add(info2);
				}
				MessageUtil.tell_player_message(player, message);
			}
			switch (battleState) {
			case eSSBS_SelectHero:
				//选定的英雄重新发送
				for (int i=0;i<this.m_battleUserInfo.length;i++)
				{
					if (this.m_battleUserInfo[i] == null || this.m_battleUserInfo[i].selectedHeroId == 0)
					{
						continue;
					}
					if (this.m_battleUserInfo[i].bIsHeroChoosed)
					{
						//然后将选择该神兽的消息广播给其他玩家
						ResSelectHeroMessage msg = new ResSelectHeroMessage();
						msg.heroId = this.m_battleUserInfo[i].selectedHeroId;
						msg.playerId = this.m_battleUserInfo[i].sPlayer.player.getId();
						MessageUtil.tell_player_message(player, msg);
					}
				}
				break;
			case eSSBS_Prepare:				
				break;
			case eSSBS_Loading:
				break;
			case eSSBS_Playing:
				info.offlineTime = 0;
				break;
			default:
				break;
			}
		}
	}
}
