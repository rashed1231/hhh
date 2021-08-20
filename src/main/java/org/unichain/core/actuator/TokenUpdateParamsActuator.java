/*
 * unichain-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * unichain-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.unichain.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.springframework.util.Assert;
import org.unichain.common.utils.Utils;
import org.unichain.core.capsule.TokenPoolCapsule;
import org.unichain.core.capsule.TransactionResultCapsule;
import org.unichain.core.db.Manager;
import org.unichain.core.exception.BalanceInsufficientException;
import org.unichain.core.exception.ContractExeException;
import org.unichain.core.exception.ContractValidateException;
import org.unichain.core.services.http.utils.Util;
import org.unichain.protos.Contract;
import org.unichain.protos.Contract.UpdateTokenParamsContract;
import org.unichain.protos.Protocol.Transaction.Result.code;

import java.util.Objects;

import static org.unichain.core.config.Parameter.ChainConstant.TOKEN_MAX_TRANSFER_FEE;
import static org.unichain.core.config.Parameter.ChainConstant.TOKEN_MAX_TRANSFER_FEE_RATE;
import static org.unichain.core.services.http.utils.Util.*;

@Slf4j(topic = "actuator")
public class TokenUpdateParamsActuator extends AbstractActuator {

  TokenUpdateParamsActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    var fee = calcFee();
    try {
      var ctx = contract.unpack(Contract.UpdateTokenParamsContract.class);
      logger.debug("TokenUpdateParams  {} ...", ctx);
      var ownerAddress = ctx.getOwnerAddress().toByteArray();
      var tokenKey = Util.stringAsBytesUppercase(ctx.getTokenName());

      TokenPoolCapsule tokenCap = dbManager.getTokenPoolStore().get(tokenKey);
      if(ctx.hasField(TOKEN_UPDATE_PARAMS_FIELD_FEE)) {
          tokenCap.setFee(ctx.getAmount());
      }

      if(ctx.hasField(TOKEN_UPDATE_PARAMS_FIELD_FEE_RATE)) {
          tokenCap.setExtraFeeRate(ctx.getExtraFeeRate());
      }

      if(ctx.hasField(TOKEN_UPDATE_PARAMS_FIELD_LOT)) {
          tokenCap.setLot(ctx.getLot());
      }

      dbManager.getTokenPoolStore().put(tokenKey, tokenCap);

      chargeFee(ownerAddress, fee);
      ret.setStatus(fee, code.SUCESS);
      logger.debug("TokenUpdateParams  {} ...DONE!", ctx);
      return true;
    } catch (Exception e) {
      logger.error("exec TokenUpdateParams got error --> ", e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
  }

  @Override
  public boolean validate() throws ContractValidateException {
      try {
          Assert.notNull(contract, "No contract!");
          Assert.notNull(dbManager, "No dbManager!");
          Assert.isTrue(contract.is(UpdateTokenParamsContract.class), "contract type error,expected type [UpdateTokenParamsContract],real type[" + contract.getClass() + "]");

          val ctx = this.contract.unpack(UpdateTokenParamsContract.class);

          Assert.isTrue(ctx.hasField(TOKEN_UPDATE_PARAMS_FIELD_OWNER_ADDR), "missing owner address");
          Assert.isTrue(ctx.hasField(TOKEN_UPDATE_PARAMS_FIELD_NAME), "missing token name");

          var ownerAddress = ctx.getOwnerAddress().toByteArray();
          var accountCap = dbManager.getAccountStore().get(ownerAddress);

          Assert.notNull(accountCap, "Invalid ownerAddress");

          Assert.isTrue (accountCap.getBalance() >= calcFee(), "Not enough balance");

          var tokenKey = Util.stringAsBytesUppercase(ctx.getTokenName());
          var tokenPool = dbManager.getTokenPoolStore().get(tokenKey);
          Assert.notNull(tokenPool, "TokenName not exist");

          Assert.isTrue (dbManager.getHeadBlockTimeStamp() < tokenPool.getEndTime(), "Token expired at: " + Utils.formatDateLong(tokenPool.getEndTime()));

          Assert.isTrue (dbManager.getHeadBlockTimeStamp() >= tokenPool.getStartTime(), "Token pending to start at: " + Utils.formatDateLong(tokenPool.getStartTime()));

          if (ctx.hasField(TOKEN_UPDATE_PARAMS_FIELD_FEE)) {
              var fee = ctx.getAmount();
              Assert.isTrue (fee >= 0 && fee <= TOKEN_MAX_TRANSFER_FEE, "invalid fee amount, should between [0, " + TOKEN_MAX_TRANSFER_FEE + "]");
          }

          if (ctx.hasField(TOKEN_UPDATE_PARAMS_FIELD_LOT)) {
              Assert.isTrue (ctx.getLot() >= 0, "invalid lot: require positive!");
          }

          if (ctx.hasField(TOKEN_UPDATE_PARAMS_FIELD_FEE_RATE)) {
              var extraFeeRate = ctx.getExtraFeeRate();
              Assert.isTrue (extraFeeRate >= 0 && extraFeeRate <= 100 && extraFeeRate <= TOKEN_MAX_TRANSFER_FEE_RATE, "invalid extra fee rate amount, should between [0, " + TOKEN_MAX_TRANSFER_FEE_RATE + "]");
          }

          return true;
      }
      catch (Exception e){
          logger.error("validate TokenUpdateParams got error -->", e);
          throw new ContractValidateException(e.getMessage());
      }
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(UpdateTokenParamsContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
      return dbManager.getDynamicPropertiesStore().getAssetIssueFee()/2;//250 unw default
  }
}