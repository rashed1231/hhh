package org.unichain.core.services.http.fullnode.servlet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.unichain.api.GrpcAPI.TransactionApprovedList;
import org.unichain.core.Wallet;
import org.unichain.core.services.http.utils.Util;
import org.unichain.protos.Protocol.Transaction;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;


@Component
@Slf4j(topic = "API")
public class GetTransactionApprovedListServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      boolean visible = Util.getVisiblePost(input);
      Transaction transaction = Util.packTransaction(input, visible);
      TransactionApprovedList reply = wallet.getTransactionApprovedList(transaction);
      if (reply != null) {
        response.getWriter().println(Util.printTransactionApprovedList(reply, visible));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }
}
