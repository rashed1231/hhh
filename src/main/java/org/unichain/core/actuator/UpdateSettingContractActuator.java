package org.unichain.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.unichain.common.utils.StringUtil;
import org.unichain.core.Wallet;
import org.unichain.core.capsule.AccountCapsule;
import org.unichain.core.capsule.ContractCapsule;
import org.unichain.core.capsule.TransactionResultCapsule;
import org.unichain.core.db.AccountStore;
import org.unichain.core.db.Manager;
import org.unichain.core.exception.BalanceInsufficientException;
import org.unichain.core.exception.ContractExeException;
import org.unichain.core.exception.ContractValidateException;
import org.unichain.protos.Contract.UpdateSettingContract;
import org.unichain.protos.Protocol.Transaction.Result.code;

//@note confirmed new fee policy
@Slf4j(topic = "actuator")
public class UpdateSettingContractActuator extends AbstractActuator {

  UpdateSettingContractActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    try {
      UpdateSettingContract usContract = contract.unpack(UpdateSettingContract.class);
      byte[] ownerAddress = usContract.getOwnerAddress().toByteArray();
      long newPercent = usContract.getConsumeUserResourcePercent();
      byte[] contractAddress = usContract.getContractAddress().toByteArray();
      ContractCapsule deployedContract = dbManager.getContractStore().get(contractAddress);

      dbManager.getContractStore().put(contractAddress, new ContractCapsule(deployedContract.getInstance().toBuilder().setConsumeUserResourcePercent(newPercent).build()));
      chargeFee(ownerAddress, fee);
      ret.setStatus(fee, code.SUCESS);
      return true;
    } catch (InvalidProtocolBufferException | BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (this.dbManager == null) {
      throw new ContractValidateException("No dbManager!");
    }
    if (!this.contract.is(UpdateSettingContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [UpdateSettingContract],real type["
              + contract
              .getClass() + "]");
    }
    final UpdateSettingContract contract;
    try {
      contract = this.contract.unpack(UpdateSettingContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    if (!Wallet.addressValid(contract.getOwnerAddress().toByteArray())) {
      throw new ContractValidateException("Invalid address");
    }
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

    AccountStore accountStore = dbManager.getAccountStore();

    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    if (accountCapsule == null) {
      throw new ContractValidateException(
          "Account[" + readableOwnerAddress + "] not exists");
    }

    long newPercent = contract.getConsumeUserResourcePercent();
    if (newPercent > 100 || newPercent < 0) {
      throw new ContractValidateException(
          "percent not in [0, 100]");
    }

    byte[] contractAddress = contract.getContractAddress().toByteArray();
    ContractCapsule deployedContract = dbManager.getContractStore().get(contractAddress);

    if (deployedContract == null) {
      throw new ContractValidateException(
          "Contract not exists");
    }

    byte[] deployedContractOwnerAddress = deployedContract.getInstance().getOriginAddress()
        .toByteArray();

    if (!Arrays.equals(ownerAddress, deployedContractOwnerAddress)) {
      throw new ContractValidateException(
          "Account[" + readableOwnerAddress + "] is not the owner of the contract");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(UpdateSettingContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
