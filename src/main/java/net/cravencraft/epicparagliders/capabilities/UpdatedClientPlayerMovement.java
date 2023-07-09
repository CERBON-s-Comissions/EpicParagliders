package net.cravencraft.epicparagliders.capabilities;

import net.cravencraft.epicparagliders.EpicParaglidersMod;
import net.cravencraft.epicparagliders.network.ModNet;
import net.cravencraft.epicparagliders.network.SyncActionMsg;
import tictim.paraglider.capabilities.ClientPlayerMovement;
import yesman.epicfight.client.ClientEngine;

public class UpdatedClientPlayerMovement extends UpdatedPlayerMovement {

    public static UpdatedClientPlayerMovement instance;
    private ClientPlayerMovement clientPlayerMovement;
    private int totalStaminaCost;

    public UpdatedClientPlayerMovement(ClientPlayerMovement clientPlayerMovement) {
        super(clientPlayerMovement);
        this.clientPlayerMovement = clientPlayerMovement;
        instance = this;
    }

    @Override
    public void update() {
        EpicParaglidersMod.LOGGER.info("Client state change & recovery delay: " + clientPlayerMovement.getState() + " | " + clientPlayerMovement.getRecoveryDelay());
        if (this.actionStaminaCost > 0) {
            this.totalStaminaCost += this.actionStaminaCost;
        }
        //TODO: Turn these two conditionals into a method. May want to use them in the client event class.
        //TODO: Ensure that the player patch is never null. Probably won't be based on how update() is called.
        if (ClientEngine.instance.getPlayerPatch().getEntityState().inaction()) {
            if (ClientEngine.instance.getPlayerPatch().getEntityState().attacking() && !this.isAttacking) {
//				this.player.getOffhandItem(); // TODO: add support for offhand weapons
                EpicParaglidersMod.LOGGER.info("Total stamina cost of the previous attack: " + totalStaminaCost);
                this.totalStaminaCost = 0;
                this.actionStaminaCost = (int)clientPlayerMovement.player.getCurrentItemAttackStrengthDelay();
                this.isAttacking = true;
            }
            else if (!ClientEngine.instance.getPlayerPatch().getEntityState().attacking() && this.isAttacking){
                this.actionStaminaCost--;
                this.isAttacking = false;
            }
        }
        else if (this.actionStaminaCost > 0){
            this.actionStaminaCost--;
        }
        updateStamina();
        addEffects();
        ModNet.NET.sendToServer(new SyncActionMsg(this.actionStaminaCost));
    }
}
